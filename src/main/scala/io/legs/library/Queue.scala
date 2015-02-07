package io.legs.library

import com.typesafe.scalalogging.Logger
import com.uniformlyrandom.scron.Scron
import io.legs.utils.EnumJson
import io.legs.utils.RedisProvider._
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import redis.api.scripting.RedisScript
import redis.protocol.Bulk

import scala.concurrent.{ExecutionContext, Future}

object Queue {

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	final val maxRetries = 5

	final val schedulePlansKey_HS = "legs:schedule:plans"
	final val queueByLabelPrefix_ZL = "legs:queue:label:"
	final def queueByLabelKey_ZL(label: String) = s"$queueByLabelPrefix_ZL$label"
	final val queueWorkingByLabelPrefix_ZL = "legs:queue:working:label:"
	final def queueWorkingByLabelKey_ZL(label: String) = s"$queueWorkingByLabelPrefix_ZL$label"
	final val queueDeferredByLabelPrefix_ZL = "legs:queue:deferred:label:"
	final def queueDeferredByLabelKey_ZL(label: String) = s"$queueDeferredByLabelPrefix_ZL$label"

	private val planAheadHours = 2
	val jobsStartValue = "1000"

	object Plans {
		val oncePerHour = "0 0 * * * *"
		val oncePer5Min = "0 0 * * * *"
	}

	val getSchedulerJob = Job(
		instructions = "scheduler",
		labels = List("scheduler"),
		input = Map.empty,
		description = "queues all scheduled jobs",
		jobType = JobType.SCHEDULE_JOB,
		priority = Priority.HIGH,
		id = "100"
	)

	private val setupRedisLua =
		s"""
		|local jobsCounterKey = '${Job.jobsCounterKey_S}'
		|local jobId = ${getSchedulerJob.id}
		|
		|if not redis.call('GET', jobsCounterKey) then
		|	redis.call('SET', jobsCounterKey, '$jobsStartValue')
		|end
		|
		|if not redis.call('HGET', '${Job.jobsData_HS}', jobId ) then
		|	redis.call('HSET', '${Job.jobsData_HS}', jobId, '${Json.toJson(getSchedulerJob).toString()}')
		|end
		""".stripMargin

	def setupRedis()(implicit ec : ExecutionContext) : Future[Unit] = {
		logger.info("setting up redis")
		writeJobSchedule(getSchedulerJob.id, "0 0 * * * *")
			.flatMap( _ => asyncRedis(_.eval(setupRedisLua)) )
			.map( _ => Unit)

	}

	def queueJobIn(job: Job, timeMillis: Long)(implicit ec : ExecutionContext) : Future[Unit] = {
		logger.info(s"persisting job in queue jobId ${job.id} time $timeMillis")
		asyncRedis{ rds => Future.sequence(
			job.labels.map( label=>
				rds.zadd(queueByLabelKey_ZL(label), (timeMillis.toDouble, job.id))
			)
		)}.map(_=> Unit)
	}

	def queueAll()(implicit ctx : ExecutionContext) : Future[Unit] = {
		logger.info("queueing all scheduled jobs")
		getJobsSchedules.flatMap {
			case schedules =>
				Future.sequence(schedules.keys.map { jobId =>
					Job.get(jobId).flatMap {
						case Some(job)=>
							val touchedJob = job.touch
							Job.createOrUpdate(touchedJob)
								.flatMap(_ => queueJobAsScheduled(touchedJob,schedules(jobId)) )
						case None =>
							logger.error(s"failed to find job id $jobId")
							Future.failed(new Throwable(s"failed to find job id $jobId"))
					}
				}).map {
					_ => logger.info(s"finished queueing #${schedules.keys.size} jobs")
				}
		}
	}

	def getScheduleForJob(id : String) : Future[Option[String]] =
		asyncRedis( _.hget[String](schedulePlansKey_HS, id) )

	def writeJobSchedule(jobId:String, schedule:String) : Future[Boolean] = {
		logger.info(s"planning jobId:$jobId with schedule: $schedule")
		asyncRedis {
			_.hset(schedulePlansKey_HS, jobId, schedule)
		}
	}

	def queueJobAsScheduled(job:Job,schedule:String) : Future[Unit] = {
		val startTimeMS = job.lastRunTime.getOrElse(DateTime.now(DateTimeZone.UTC).getMillis)
		val endTimeMS = DateTime.now(DateTimeZone.UTC).plusHours(planAheadHours).getMillis
		val times = Scron.parse(schedule, startTimeMS / 1000, endTimeMS / 1000).toList
		Future.sequence(times.map { time=>
			Job.getNextJobId.flatMap { newJobId =>
				logger.info(s"queueing for parentId:${job.id} childId:$newJobId time:${time * 1000}")
				val childJob = job.forChildWithId(newJobId)
				Job.createOrUpdate(childJob)
					.flatMap(_=> queueJobIn(childJob, time * 1000))
			}
		}).map( _=> logger.info(s"done queueing job ID:${job.id}") )
	}

	def getNextJobFromQueue(labels: List[String]) : Future[Option[Job]] = {
		logger.info(s"getting next job from queue for labels:${labels.mkString(",")}")
		asyncRedis (
			_.evalshaOrEval(nextJobFromQueueLua,Nil,Seq(Json.toJson(labels).toString(),DateTime.now(DateTimeZone.UTC).getMillis.toString))
		) map {
			case b: Bulk => b.toOptString.map(s=>Json.parse(s.toString).as[Job])
			case _ => None
		}
	}

	def queueJobImmediately(job: Job) : Future[Unit] =
		queueJobIn( job, DateTime.now(DateTimeZone.UTC).getMillis )


	def getJobsSchedules()(implicit ec : ExecutionContext) =
		asyncRedis( _.hgetall[String](schedulePlansKey_HS) )


	def removeJob(job: Job) : Future[Unit] =
		asyncRedis { rc =>
			Future.sequence(job.labels.map(l=>{
				rc.zrem(queueByLabelKey_ZL(l),job.id)
				rc.zrem(queueWorkingByLabelKey_ZL(l),job.id)
				rc.zrem(queueDeferredByLabelKey_ZL(l),job.id)
			}) ::: List(
				rc.hdel(schedulePlansKey_HS, job.id)
			)).map {
				_ => logger.info(s"finished deleting artifacts for job${job.id}")
			}
		}

	private lazy val nextJobFromQueueLua = RedisScript(s"""
		local queueByLabelPrefix = "$queueByLabelPrefix_ZL"
		local queueWorkingByLabelPrefix = "$queueWorkingByLabelPrefix_ZL"
		local queueDeferredByLabelPrefix = "$queueDeferredByLabelPrefix_ZL"
		local jobDataKey = "${Job.jobsData_HS}"
		local maxRetries = $maxRetries
		local labels = cjson.decode(ARGV[1])
		local currTimeMS = 0 + ARGV[2]
		local jobStatuses = cjson.decode('${EnumJson.toJsonMap(JobStatus).toString()}')

		local function verifyFoundJob(jobId)
			local jobData = cjson.decode(redis.call('HGET', jobDataKey ,jobId))

			--check if too many retries
			if jobData.retries >= maxRetries then
				-- defer job
				jobData.status = jobStatuses.DEFERRED
				for _i, deferLabel in ipairs(jobData.labels) do
					redis.call('ZADD', queueDeferredByLabelPrefix .. deferLabel, currTimeMS, jobData.id)
					redis.call('ZREM', queueWorkingByLabelPrefix .. deferLabel, jobData.id)
				end
				local jobDataEncoded = cjson.encode(jobData)
				redis.call('HSET', jobDataKey ,jobData.id, jobDataEncoded)
				return nil
			else
				-- looks good, accept job
				jobData.retries = jobData.retries +1
				jobData.status = jobStatuses.WORKING
				jobData.lastRunTime = currTimeMS
				for _i, _v in ipairs(jobData.labels) do
					redis.call('ZREM', queueByLabelPrefix .. _v, jobData.id)
					redis.call('ZADD', queueWorkingByLabelPrefix .. _v, currTimeMS, jobData.id)
				end
				local jobDataEncoded = cjson.encode(jobData)
				redis.call('HSET', jobDataKey ,jobData.id, jobDataEncoded)
				return jobData
			end
		end

		local function findNext(label)
			local oldJobs = redis.call('ZRANGEBYSCORE', queueWorkingByLabelPrefix .. label, 0, currTimeMS - 5 * 60 * 1000, 'LIMIT',0,1)
			if #oldJobs > 0 then
				-- try to find old job from the queue
				local oldJobId = oldJobs[1]
				local verifiedJob = verifyFoundJob(oldJobId)
				if verifiedJob then return verifiedJob
				else findNext(label) end
			else
				-- try to find a normal job for this label
				local jobIDFromQeueueTable = redis.call('ZRANGEBYSCORE', queueByLabelPrefix .. label, 0, currTimeMS, 'LIMIT',0,1)
				if #jobIDFromQeueueTable > 0 then
					local jobIDFromQeueue = jobIDFromQeueueTable[1]
					return verifyFoundJob(jobIDFromQeueue)
				end
			end
		end
		for i, label in ipairs(labels) do
			local found = findNext(label)
			if found then return cjson.encode(found) end
		end
		-- if we got this far, there are no jobs in the queue
		return nil
		"""	)


}

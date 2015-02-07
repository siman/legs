package io.legs.specialized

import com.typesafe.scalalogging.Logger
import com.uniformlyrandom.scron.Scron
import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import io.legs.scheduling.{Job, JobStatus, JobType, Priority}
import io.legs.utils.RedisProvider._
import io.legs.utils.{EnumJson, JsonFriend}
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, Json}
import redis.api.scripting.RedisScript
import redis.protocol.Bulk

import scala.concurrent._

object QueueSpecialized extends Specialization {

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
		writeJobPlan(getSchedulerJob.id, "0 0 * * * *")
			.flatMap( _ => asyncRedis(_.eval(setupRedisLua)) )
			.map( _ => Unit)

	}

	def queueJobImmediately(job: Job) : Future[Unit] =
		persistJobQueue( job, DateTime.now(DateTimeZone.UTC).getMillis )


	private def getScheduleForJob(id : String) : Future[Option[String]] =
		asyncRedis( _.hget[String](schedulePlansKey_HS, id) )


	def getJobsSchedules()(implicit ec : ExecutionContext) =
		asyncRedis( _.hgetall[String](schedulePlansKey_HS) )

	private def persistJobQueue(job: Job, timeMillis: Long)(implicit ec : ExecutionContext) : Future[Unit] = {
		logger.info(s"persisting job in queue jobId ${job.id} time $timeMillis")
		asyncRedis{ rds => Future.sequence(
			job.labels.map( label=>
				rds.zadd(queueByLabelKey_ZL(label), (timeMillis.toDouble, job.id))
			)
		)}.map(_=> Unit)
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

	def getNextJobFromQueue(labels: List[String]) : Future[Option[Job]] = {
		logger.info(s"getting next job from queue for labels:${labels.mkString(",")}")
		asyncRedis (
			_.evalshaOrEval(nextJobFromQueueLua,Nil,Seq(Json.toJson(labels).toString(),DateTime.now(DateTimeZone.UTC).getMillis.toString))
		) map {
			case b: Bulk => b.toOptString.map(s=>Json.parse(s.toString).as[Job])
			case _ => None
		}
	}

	@LegsFunctionAnnotation(
		details = "Add a job to the FIFO queue",
		yieldType = "String",
		yieldDetails = "new job id is yielded"
	)
	def ADD_JOB(state: Specialization.State,
		instructions: String @LegsParamAnnotation("name of instructions file"),
		description: String @LegsParamAnnotation("job description"),
		labels: List[JsString] @LegsParamAnnotation("list of labels to be used to target specific queues"),inputIndices:List[JsString] @LegsParamAnnotation("values to be taken from the state as input for the new job")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Job.getNextJobId.map { nextJobId =>
			val inputKeys =  inputIndices.map(_.value)
			if (inputKeys.filterNot(i => state.keys.exists(i.equals)).nonEmpty){
				throw new Exception("could not find all input values in state, missing:" + inputKeys.filterNot(i=>state.keys.exists(i.equals)).mkString(",") )
			} else {
				val inputs = inputKeys.zip(inputKeys.map(iName=> JsonFriend.jsonify(state(iName)))).toMap

				Job(
					instructions,
					labels.map(_.value),
					inputs,
					description,
					JobType.AD_HOC,
					Priority.LOW,
					nextJobId
				)
			}
		}.flatMap { job =>
			Job.createOrUpdate(job)
				.flatMap(_ => persistJobQueue(job,DateTime.now(DateTimeZone.UTC).getMillis))
				.map(_ => Yield(Some(job.id)))
		}

	private def writeJobPlan(jobId:String, schedule:String) : Future[Boolean] = {
		logger.info(s"planning jobId:$jobId with schedule: $schedule")
		asyncRedis {
			_.hset(schedulePlansKey_HS, jobId, schedule)
		}
	}

	def deleteJob(job: Job) : Future[Unit] =
		Job.delete(job.id)
			.flatMap { _ =>
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
			}

	def retryJob(job: Job) : Future[Boolean] =
		Job.createOrUpdate(job.incRetry)

	@LegsFunctionAnnotation(
		details = "create a schedule plan for a job",
		yieldType = None,
		yieldDetails = "nothing is returned"
	)
	def PLAN(state: Specialization.State,
		schedule: String @LegsParamAnnotation("the schedule, in cron format"),
		jobId: String @LegsParamAnnotation("job id to be used")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		writeJobPlan(jobId, schedule)
			.map(_=> Yield(None))

	private def queueAScheduleJob(job:Job,schedule:String) : Future[Unit] = {
		val startTimeMS = job.lastRunTime.getOrElse(DateTime.now(DateTimeZone.UTC).getMillis)
		val endTimeMS = DateTime.now(DateTimeZone.UTC).plusHours(planAheadHours).getMillis
		val times = Scron.parse(schedule, startTimeMS / 1000, endTimeMS / 1000).toList
		Future.sequence(times.map { time=>
			Job.getNextJobId.flatMap { newJobId =>
				logger.info(s"queueing for parentId:${job.id} childId:$newJobId time:${time * 1000}")
				val childJob = job.forChildWithId(newJobId)
				Job.createOrUpdate(childJob)
					.flatMap(_=> persistJobQueue(childJob, time * 1000))
			}
		}).map( _=> logger.info(s"done queueing job ID:${job.id}") )
	}

	@LegsFunctionAnnotation(
		details = "queue a scheduled job for execution",
		yieldType = None,
		yieldDetails = ""
	)
	def QUEUE(state: Specialization.State,
		jobId : String @LegsParamAnnotation("the job ID to be queued according to its schedule plan")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Job.get(jobId).flatMap {
			case Some(job)=>
				getScheduleForJob(jobId).flatMap {
					case Some(jobSchedule) =>
						val jobTouched = job.touch
						Job.createOrUpdate(jobTouched)
							.flatMap(_=> queueAScheduleJob(jobTouched,jobSchedule))
					case None => throw new Exception("could not find schedule for job ID" + jobId)
				}
			case None => throw new Exception("could not find job for ID" + jobId)
		}.map {
			_ => Yield(None)
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
								.flatMap(_ => queueAScheduleJob(touchedJob,schedules(jobId)) )
						case None =>
							logger.error(s"failed to find job id $jobId")
							Future.failed(new Throwable(s"failed to find job id $jobId"))
					}
				}).map {
					_ => logger.info(s"finished queueing #${schedules.keys.size} jobs")
				}
		}
	}

	@LegsFunctionAnnotation(
		details = "queue all scheduled jobs according to their schedule",
		yieldType = None,
		yieldDetails = ""
	)
	def QUEUE_ALL(state: Specialization.State)(implicit ctx : ExecutionContext) : RoutableFuture =
		queueAll()
			.map( _ => Yield(None) )


}

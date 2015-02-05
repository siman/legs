package io.legs.specialized

import com.typesafe.scalalogging.Logger
import com.uniformlyrandom.scron.Scron
import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import io.legs.scheduling.{Job, JobStatus, JobType, Priority}
import io.legs.utils.RedisProvider._
import io.legs.utils.{EnumJson, JsonFriend, RedisProvider}
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsString, Json}
import redis.api.scripting.RedisScript
import redis.protocol.{Bulk, RedisReply}

import scala.concurrent._

object Queue extends Specialization {

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	final val jobsData_HS = "legs:jobs"
	final val jobsCounterKey_S = "legs:jobs:counter"
	final val maxRetries = 5

	final val schedulePlansKey_HS = "legs:schedule:plans"

	final val queueByLabelPrefix_ZL = "legs:queue:label:"
	final def queueByLabelKey_ZL(label: String) = s"$queueByLabelPrefix_ZL$label"
	final val queueWorkingByLabelPrefix_ZL = "legs:queue:working:label:"
	final def queueWorkingByLabelKey_ZL(label:String) = s"$queueWorkingByLabelPrefix_ZL$label"
	final val queueDeferredByLabelPrefix_ZL = "legs:queue:deferred:label:"
	final def queueDeferredByLabelKey_ZL(label:String) = s"$queueDeferredByLabelPrefix_ZL$label"

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
			|local jobsCounterKey = '$jobsCounterKey_S'
			|local jobId = ${getSchedulerJob.id}
			|
			|if not redis.call('GET', jobsCounterKey) then
			|	redis.call('SET', jobsCounterKey, '$jobsStartValue')
			|end
			|
			|if not redis.call('HGET', '$jobsData_HS', jobId ) then
			|	redis.call('HSET', '$jobsData_HS', jobId, '${Json.toJson(getSchedulerJob).toString()}')
			|end
		""".stripMargin

	def setupRedis() = {
		logger.info("setting up redis")
		writeJobPlan(getSchedulerJob.id, "0 0 * * * *")
		RedisProvider.redisPool.eval(setupRedisLua)
	}

	def persistJob(job:Job) =
		RedisProvider.redisPool.hset(jobsData_HS,job.id,Json.stringify(Json.toJson(job)))

	def queueJobImmediately(job: Job) =
		persistJobQueue(job, DateTime.now(DateTimeZone.UTC).getMillis)


	def getJobAsync(id : String)(implicit ec : ExecutionContext) : Future[Option[Job]] =
		asyncRedis(_.hget[String](jobsData_HS,id)
			map { _.map( jobString => Json.parse(jobString).as[Job] ) } )


	def getJob(id: String) : Option[Job] =
		RedisProvider.blockingRedis
			{ _.hget[String](jobsData_HS,id) } match {
				case Some(jobString) =>
					Json.parse(jobString).asOpt[Job]
				case None => None
			}

	def deleteJob(job: Job) {

		logger.info(s"deleting job ${job.id} from queue")

		RedisProvider.blockingList {
			c=>
				job.labels.map(l=>{
					c.zrem(queueByLabelKey_ZL(l),job.id)
					c.zrem(queueWorkingByLabelKey_ZL(l),job.id)
					c.zrem(queueDeferredByLabelKey_ZL(l),job.id)
				}) ::: List(
					c.hdel(jobsData_HS, job.id),
					c.hdel(schedulePlansKey_HS, job.id)
				)
		}


	}

	private def getScheduleForJob(id : String) : Option[String] =
		RedisProvider.blockingRedis {
			_.hget[String](schedulePlansKey_HS, id)
		}


	def getAllScheduledJobs =
		RedisProvider.blockingRedis {
			_.hgetall[String](schedulePlansKey_HS)
		}

	def getNextJobId : String =
		RedisProvider.blockingRedis {
			_.incr(jobsCounterKey_S)
		}.toString

	private def persistJobQueue(job: Job, timeMillis: Long ){
		logger.info(s"persisting job in queue jobId ${job.id} time $timeMillis")
		job.labels.foreach(label=>
			RedisProvider.redisPool.zadd(queueByLabelKey_ZL(label), (timeMillis.toDouble, job.id))
		)
	}

	def retryJob(job: Job){
		persistJob(job.copy(retries = job.retries+1))
	}

	private lazy val nextJobFromQueueLua = RedisScript(s"""
		local queueByLabelPrefix = "$queueByLabelPrefix_ZL"
		local queueWorkingByLabelPrefix = "$queueWorkingByLabelPrefix_ZL"
		local queueDeferredByLabelPrefix = "$queueDeferredByLabelPrefix_ZL"
		local jobDataKey = "$jobsData_HS"
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

	def getNextJobFromQueue(labels: List[String]) : Option[Job] = {
		logger.info(s"getting next job from queue for labels:${labels.mkString(",")}")
		RedisProvider.blockingRedis[RedisReply] {
			_.evalshaOrEval(nextJobFromQueueLua,Nil,Seq(Json.toJson(labels).toString(),DateTime.now(DateTimeZone.UTC).getMillis.toString))
		} match {
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
		labels: List[JsString] @LegsParamAnnotation("list of labels to be used to target specific queues"),
		inputIndices:List[JsString] @LegsParamAnnotation("values to be taken from the state as input for the new job")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {

			val inputKeys =  inputIndices.map(_.value)
			if (inputKeys.filterNot(i => state.keys.exists(i.equals)).nonEmpty){
				throw new Exception("could not find all input values in state, missing:"
					+ inputKeys.filterNot(i=>state.keys.exists(i.equals)).mkString(",") )
			} else {
				val inputs = inputKeys.zip(inputKeys.map(iName=> JsonFriend.jsonify(state(iName)))).toMap

				val newJobId = getNextJobId

				val job = Job(
					instructions,
					labels.map(_.value),
					inputs,
					description,
					JobType.AD_HOC,
					Priority.LOW,
					newJobId
				)

				persistJob(job)
				persistJobQueue(job,DateTime.now(DateTimeZone.UTC).getMillis)
				Yield(Some(newJobId))
			}
		}

	private def writeJobPlan(jobId:String, schedule:String) = {
		logger.info(s"planning jobId:$jobId with schedule: $schedule")
		RedisProvider.redisPool.hset(schedulePlansKey_HS, jobId, schedule)
	}

	@LegsFunctionAnnotation(
		details = "create a schedule plan for a job",
		yieldType = None,
		yieldDetails = "nothing is returned"
	)
	def PLAN(state: Specialization.State,
		schedule: String @LegsParamAnnotation("the schedule, in cron format"),
		jobId: String @LegsParamAnnotation("job id to be used")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			blocking { writeJobPlan(jobId, schedule) }
			Yield(None)
		}

	private def queueAScheduleJob(job:Job,schedule:String) {
		val startTimeMS = job.lastRunTime.getOrElse(DateTime.now(DateTimeZone.UTC).getMillis)
		val endTimeMS = DateTime.now(DateTimeZone.UTC).plusHours(planAheadHours).getMillis
		Scron.parse(schedule, startTimeMS / 1000, endTimeMS / 1000).foreach {
			time => {
				val nextJobId = getNextJobId
				logger.info(s"queueing for parentId:${job.id} childId:$nextJobId time:${time * 1000}")
				val childJob = Job.createChildWithId(job, nextJobId)
				persistJob(childJob)
				persistJobQueue(childJob, time * 1000)
			}
		}
		logger.info(s"done queueing job ID:${job.id}")
	}

	@LegsFunctionAnnotation(
		details = "queue a scheduled job for execution",
		yieldType = None,
		yieldDetails = ""
	)
	def QUEUE(state: Specialization.State,
		jobId : String @LegsParamAnnotation("the job ID to be queued according to its schedule plan")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val jobOpt = getJob(jobId)
			val jobSchedule = getScheduleForJob(jobId)
			if (jobOpt.isEmpty) throw new Exception("could not find job for ID" + jobId)
			else if (jobSchedule.isEmpty) throw new Exception("could not find schedule for job ID" + jobId)
			else {
				val job = jobOpt.get.touch
				blocking {
					persistJob(job)
					queueAScheduleJob(job,jobSchedule.get)
				}
				Yield(None)
			}
		}

	def queueAll(){
		logger.info("queueing all scheduled jobs")
		val scheduledJobs = getAllScheduledJobs
		if (scheduledJobs.nonEmpty){
			scheduledJobs.keys.foreach { jobId =>
				val jobOpt = getJob(jobId)
				jobOpt match {
					case Some(fetchedJob)=>
						val job = fetchedJob.touch
						persistJob(job)
						queueAScheduleJob(job,scheduledJobs(jobId))
					case None => logger.error(s"failed to find job id $jobId")
				}

			}
		}
	}

	@LegsFunctionAnnotation(
		details = "queue all scheduled jobs according to their schedule",
		yieldType = None,
		yieldDetails = ""
	)
	def QUEUE_ALL(state: Specialization.State)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			queueAll()
			Yield(None)
		}

}

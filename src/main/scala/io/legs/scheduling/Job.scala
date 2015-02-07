package io.legs.scheduling

import com.typesafe.scalalogging.Logger
import io.legs.utils.{EnumJson, RedisProvider}
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}
import RedisProvider._

import scala.concurrent.{ExecutionContext, Future}

case class Job(
	instructions: 		String,
	labels: 			List[String],
	input: 				Map[String,JsValue],
	description: 		String,
	jobType: 			JobType.Value,
	priority: 			Priority.Value,
	id:					String,

	status: 			JobStatus.Value = JobStatus.QUEUED,
	parentId:			Option[String] = None,
	retries:			Int = 0,
	lastRunTime: 		Option[Long] = None,
	creationTime: 		Long = DateTime.now(DateTimeZone.UTC).getMillis,
	uuid: 				String = java.util.UUID.randomUUID.toString
) {

	def touch = copy( lastRunTime = Some(DateTime.now(DateTimeZone.UTC).getMillis) )

	def forChildWithId(childId : String) = copy( jobType = JobType.SCHEDULED_CHILD, id = childId, parentId = Some(id) )

	def incRetry = copy(retries = retries + 1)

}


object JobStatus extends Enumeration {
	type JobStatus = Value
	val QUEUED, WORKING, DONE, DEFERRED = Value

	implicit val fmt = EnumJson.enumFormat(JobStatus)
}

object JobType extends Enumeration {
	type JobType = Value
	val SCHEDULE_JOB, SCHEDULED_CHILD, AD_HOC = Value

	implicit val fmt = EnumJson.enumFormat(JobType)
}

object Priority extends Enumeration {
	type Priority = Value
	val LOW, HIGH = Value

	implicit val fmt = EnumJson.enumFormat(Priority)
}

object Job {

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	final val jobsData_HS = "legs:jobs"
	final val jobsCounterKey_S = "legs:jobs:counter"


	implicit val fmt = Json.format[Job]

	def createOrUpdate(job:Job) : Future[Boolean] =
		asyncRedis(_.hset(Job.jobsData_HS,job.id,Json.stringify(Json.toJson(job))))

	def get(id: String)(implicit ec : ExecutionContext) : Future[Option[Job]] =
		RedisProvider.asyncRedis {
			_.hget[String](Job.jobsData_HS,id)
		}.map {
			_.map(jobStr => Json.parse(jobStr).as[Job] )
		}

	def delete(id : String)(implicit ec : ExecutionContext) : Future[Int] = {
		logger.info(s"deleting job $id from")
		RedisProvider.asyncRedis(_.del(jobsData_HS, id))
			.map(_.toInt)
	}

	def getNextJobId()(implicit ec : ExecutionContext) : Future[String] =
		RedisProvider.asyncRedis(_.incr(jobsCounterKey_S))
			.map(_.toString)

}
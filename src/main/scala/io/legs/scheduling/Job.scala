package io.legs.scheduling

import io.legs.utils.EnumJson
import play.api.libs.json.{JsValue, Json}
import org.joda.time.{DateTimeZone, DateTime}

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
	def touch = this.copy( lastRunTime = Some(DateTime.now(DateTimeZone.UTC).getMillis) )
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

	implicit val fmt = Json.format[Job]

	def createChildWithId(job: Job, id: String) : Job = {
		Job(job.instructions, job.labels, job.input, job.description, JobType.SCHEDULED_CHILD, job.priority, id, parentId = Some(job.id))
	}

}
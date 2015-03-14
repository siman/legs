package io.legs.ui.shared.model

import io.legs.library.{JobStatus, JobType, Priority}
import play.api.libs.json.Json

case class JobLike(
	instructions: 		String,
	labels: 			List[String],
	input: 				Map[String,String],
	description: 		String,
	jobType: 			JobType.Value,
	priority: 			Priority.Value,
	id:					String,

	status: 			JobStatus.Value = JobStatus.QUEUED,
	parentId:			Option[String] = None,
	retries:			Int = 0,
	lastRunTime: 		Option[Long] = None,
	creationTime: 		Long = System.currentTimeMillis,
	uuid: 				String = java.util.UUID.randomUUID.toString
)


object JobLike {
	implicit lazy val fmt = Json.format[JobLike]

	val a = Int.MaxValue
}
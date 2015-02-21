package io.legs.ui.shared.model

import io.legs.library.Job
import play.api.libs.json.Json

case class ScheduledJob(
	jobId :String,
	schedule :String,
	jobData : Option[Job]
)

object ScheduledJob {
	implicit val fmt = Json.format[ScheduledJob]
}
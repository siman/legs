package io.legs.ui.shared.model

case class ScheduledJob(
	jobId :String,
	schedule :String,
	jobData : JobLike
)
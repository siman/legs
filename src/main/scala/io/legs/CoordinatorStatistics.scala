package io.legs

import org.joda.time.{DateTimeZone, DateTime}

case class CoordinatorStatistics(
			jobsCompleted: Int = 0,
			jobsSucceeded : Int = 0,
			jobsFailed: Int = 0,
			lastWorkQueueCheck : Option[DateTime] = None
) {
	def succeeded = this.copy(jobsCompleted = this.jobsCompleted + 1, jobsSucceeded = this.jobsSucceeded + 1)
	def failed = this.copy(jobsCompleted = this.jobsCompleted + 1, jobsFailed = this.jobsFailed + 1)
	def touch = this.copy(lastWorkQueueCheck = Some(DateTime.now(DateTimeZone.UTC)))
}

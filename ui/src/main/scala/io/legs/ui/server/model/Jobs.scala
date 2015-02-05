package io.legs.ui.server.model

import io.legs.scheduling.Job
import io.legs.specialized.Queue

import scala.concurrent.{ExecutionContext, Future}

object Jobs {

	def getScheduledJobs()(implicit ec : ExecutionContext) : Future[List[ScheduledJob]] =
		Future { Queue.getAllScheduledJobs.toList }.flatMap {
			case queuedJobs =>
				Future.sequence(queuedJobs.map(qj=>Queue.getJobAsync(qj._1))).map {
					jobsOpts => queuedJobs.map {
						case (jobId,jobSchedule) =>
							ScheduledJob(jobId,jobSchedule,jobsOpts.find(jOpt=> jOpt.isDefined && jOpt.get.id == jobId).getOrElse(None))
					}
				}
		}

}

case class ScheduledJob(
	jobId :String,
	schedule :String,
	jobData : Option[Job]
)


package io.legs.ui.server.model

import io.legs.scheduling.Job
import io.legs.specialized.QueueSpecialized

import scala.concurrent.{ExecutionContext, Future}

object Jobs {

	def getScheduledJobs()(implicit ec : ExecutionContext) : Future[List[ScheduledJob]] =
		QueueSpecialized.getJobsSchedules.flatMap {
			case schedules =>
				Future.sequence( schedules.keys.map(jId=>Job.get(jId)) )
					.map { jobsOpts =>
						schedules.toList.map {
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


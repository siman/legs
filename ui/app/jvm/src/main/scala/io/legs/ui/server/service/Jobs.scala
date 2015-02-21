package io.legs.ui.server.service

import io.legs.library.{Job, Queue}
import io.legs.ui.shared.model.ScheduledJob

import scala.concurrent.{ExecutionContext, Future}

object Jobs {

	def getScheduledJobs()(implicit ec : ExecutionContext) : Future[List[ScheduledJob]] =
		Queue.getJobsSchedules.flatMap {
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




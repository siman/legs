package io.legs.ui.server.service

import io.legs.library.{Queue, Job, JobType, Priority}
import io.legs.ui.shared.model.{JobLike, ScheduledJob}
import io.nozzler.{CRUDMeta, CRUDService}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

object JobsService extends CRUDService {

	type ModelType = ScheduledJob

	val fmt = Json.format[ModelType]

	val meta = CRUDMeta[ModelType]

	override def create(m: ModelType, query: Map[String, String])(implicit executor: ExecutionContext): Future[String] = ???

	override def read(uid: Option[String], query: Map[String, String])(implicit executor: ExecutionContext): Future[List[ModelType]] =
		Queue.getJobsSchedules.flatMap {
			case schedules =>
				Future.sequence( schedules.keys.map(jId=>Job.get(jId).map(jOpt => jId -> jOpt )) )
					.map { jobsOpts =>
						schedules.toList.map { case (jobId, schedule)=>
							val job = jobsOpts.find(_._1 == jobId).get._2.getOrElse(throw new Throwable(s"could not find job for id:$jobId"))
							ScheduledJob(jobId,schedule,transformJobLike(job))
						}
					}
		}

	override def update(m: ModelType, query: Map[String, String])(implicit executor: ExecutionContext): Future[Unit] = ???

	override def delete(uid: String, query: Map[String, String])(implicit executor: ExecutionContext): Future[Boolean] = ???

	private def transformJobLike(job : Job) : JobLike =
		JobLike(
			job.instructions,
			job.labels,
			job.input.map{ case (k,v) => k -> v.toString },
			job.description,
			job.jobType,
			job.priority,
			job.id,
			job.status,
			job.parentId,
			job.retries,
			job.lastRunTime,
			job.creationTime,
			job.uuid
		)

}




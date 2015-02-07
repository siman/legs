package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import io.legs.library.{Priority, JobType, Job}
import io.legs.utils.JsonFriend
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.JsString

import scala.concurrent._

object QueueSpecialized extends Specialization {

	import io.legs.library.Queue._

	@LegsFunctionAnnotation(
		details = "Add a job to the FIFO queue",
		yieldType = "String",
		yieldDetails = "new job id is yielded"
	)
	def ADD_JOB(state: Specialization.State,
		instructions: String @LegsParamAnnotation("name of instructions file"),
		description: String @LegsParamAnnotation("job description"),
		labels: List[JsString] @LegsParamAnnotation("list of labels to be used to target specific queues"),inputIndices:List[JsString] @LegsParamAnnotation("values to be taken from the state as input for the new job")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Job.getNextJobId.map { nextJobId =>
			val inputKeys =  inputIndices.map(_.value)
			if (inputKeys.filterNot(i => state.keys.exists(i.equals)).nonEmpty){
				throw new Exception("could not find all input values in state, missing:" + inputKeys.filterNot(i=>state.keys.exists(i.equals)).mkString(",") )
			} else {
				val inputs = inputKeys.zip(inputKeys.map(iName=> JsonFriend.jsonify(state(iName)))).toMap

				Job(
					instructions,
					labels.map(_.value),
					inputs,
					description,
					JobType.AD_HOC,
					Priority.LOW,
					nextJobId
				)
			}
		}.flatMap { job =>
			Job.createOrUpdate(job)
				.flatMap( _ => queueJobIn(job,DateTime.now(DateTimeZone.UTC).getMillis))
				.map( _ => Yield(Some(job.id)))
		}



	@LegsFunctionAnnotation(
		details = "create a schedule plan for a job",
		yieldType = None,
		yieldDetails = "nothing is returned"
	)
	def PLAN(state: Specialization.State,
		schedule: String @LegsParamAnnotation("the schedule, in cron format"),
		jobId: String @LegsParamAnnotation("job id to be used")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		writeJobSchedule(jobId, schedule)
			.map(_=> Yield(None))

	@LegsFunctionAnnotation(
		details = "queue a scheduled job for execution",
		yieldType = None,
		yieldDetails = ""
	)
	def QUEUE(state: Specialization.State,
		jobId : String @LegsParamAnnotation("the job ID to be queued according to its schedule plan")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Job.get(jobId).flatMap {
			case Some(job)=>
				getScheduleForJob(jobId).flatMap {
					case Some(jobSchedule) =>
						val jobTouched = job.touch
						Job.createOrUpdate(jobTouched)
							.flatMap(_=> queueJobAsScheduled(jobTouched,jobSchedule))
					case None => throw new Exception("could not find schedule for job ID" + jobId)
				}
			case None => throw new Exception("could not find job for ID" + jobId)
		}.map {
			_ => Yield(None)
		}

	@LegsFunctionAnnotation(
		details = "queue all scheduled jobs according to their schedule",
		yieldType = None,
		yieldDetails = ""
	)
	def QUEUE_ALL(state: Specialization.State)(implicit ctx : ExecutionContext) : RoutableFuture =
		queueAll()
			.map( _ => Yield(None) )


}

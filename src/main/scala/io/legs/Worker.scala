package io.legs

import scala.concurrent._
import scala.util.Try
import scala.async.Async.{async, await}
import scala.concurrent.duration.Duration
import akka.actor.{Props, ActorRef, Actor}
import io.legs.scheduling.{JobType, Job}
import io.legs.utils.InstructionsFileResolver
import io.legs.specialized.Queue
import io.legs.Coordinator.JobFailed
import scala.util.Failure
import scala.Some
import io.legs.Coordinator.JobSuccess
import scala.util.Success
import java.util.logging.{Level, Logger}
import io.legs.Specialization.{RoutableFuture, Yield}


class Worker(coordinator: ActorRef, job: Job) extends Actor {

	import Worker._

	def receive  = {
		case StartWork => startWork()
	}

	def startWork(){
		logger.info("starting work on jobId:" + job.id )
		InstructionsFileResolver.getFile(job.instructions) match {
			case Some(instructionsStr) =>
				Worker.execute(instructionsStr,job.input) match {
					case Success(v)=> workerSuccess()
					case Failure(e)=> workerFail("failed executing job",Some(e))
				}
			case None => workerFail("could not find instructions file")
		}
	}

	private def workerSuccess(){
		logger.info(s"successfully finished jobId:${job.id}")
		job.jobType match {
			case JobType.SCHEDULED_CHILD | JobType.AD_HOC  =>
				logger.info(s"cleaning up ${job.jobType} jobId:${job.id} ")
				Queue.deleteJob(job)
			case ignore =>
		}
		coordinator ! JobSuccess(job.id)
		stop()
	}

	private def workerFail(message:String, e:Option[Throwable] = None){
		if (e.isDefined) logger.log(Level.WARNING,s"failing jobId:${job.id}  with message:$message",e)
		else logger.log(Level.WARNING,s"failing jobId:${job.id}  with message:$message")
		Queue.retryJob(job)
		coordinator ! JobFailed(job.id, message)
		stop()
	}

	private def stop(){
		logger.info(s"stopping worker for jobId:${job.id}")
		context.stop(context.self)
	}


}

object Worker {

	import scala.concurrent.ExecutionContext.Implicits.global

	case class StartWork()

	private lazy val logger = Logger.getLogger(this.getClass.getSimpleName)

	def props(coordinator: ActorRef, job:Job) : Props = Props(new Worker(coordinator,job))

	def execute(jsonString: String, state:Map[String,Any] = Map()): Try[Yield] = {

		val steps = Step.from(jsonString)

		try {
			Await.result(walk(steps, state),Duration("5 minutes"))
		} catch {
			case e : TimeoutException => Failure(new Throwable("time ran out while working",e))
			case e : Exception => Failure(e)
		}

	}

	private case class IterateState(yielded:Map[String,Any],state:Map[String,Any],errOpt:Option[String] = None) {
		lazy val stateAndYield = yielded ++ state
	}

	def walk(steps:List[Step],state:Map[String,Any] = Map()) : RoutableFuture = async {
		steps match {
			case x::xs =>
				await(Specialization.executeStep(x,state)) match {
					case Success(v) =>
						if (xs.isEmpty) Success(v)
						else {
							val recState = if (x.yields.isDefined && v.valueOpt.isDefined) state + (x.yields.get -> v.valueOpt.get) else state
							await(walk(xs,recState))
						}
					case Failure(e) => Failure(e)
				}
			case Nil => Success(Yield(None))
		}
	}

}
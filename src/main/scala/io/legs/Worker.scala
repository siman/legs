package io.legs

import java.util.logging.{Level, Logger}

import akka.actor.{Actor, ActorRef, Props}
import io.legs.Coordinator.{JobFailed, JobSuccess}
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.scheduling.{Job, JobType}
import io.legs.specialized.Queue
import io.legs.utils.InstructionsFileResolver

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


class Worker(coordinator: ActorRef, job: Job) extends Actor {

	import io.legs.Worker._

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

	def execute(jsonString: String, state:Map[String,Any] = Map()): Try[Yield] =
		 Try { Await.result(executeAsync(jsonString, state),Duration("5 minutes")) }

	def executeAsync(jsonString: String, state:Map[String,Any] = Map()): Future[Yield] = {

		val steps = Step.from(jsonString)

		try {
			walk(steps, state)
		} catch {
			case e : TimeoutException => throw new Throwable("time ran out while working",e)
			case e : Exception => throw e
		}

	}

	private case class IterateState(yielded:Map[String,Any],state:Map[String,Any],errOpt:Option[String] = None) {
		lazy val stateAndYield = yielded ++ state
	}

	def walk(steps:List[Step],state:Map[String,Any] = Map()) : RoutableFuture =
		steps match {
			case x::xs =>
				Specialization.executeStep(x,state).flatMap {
					case Yield(Some(yielded)) if x.transform.isDefined =>
						val transformSteps =
							Step.from(x.transform.get) match {
								case y::ys => y.copy(yields = Some("$v"))::ys
								case ignore => ignore
							}
						walk(transformSteps, state + ("$v" -> yielded))
					case dontCare => Future.successful(dontCare)
				}.flatMap {
					case yielded =>
						if (xs.isEmpty) Future.successful(yielded)
						else {
							val recState =
								if (x.yields.isDefined && yielded.valueOpt.isDefined) state + (x.yields.get -> yielded.valueOpt.get) else state
							walk(xs,recState)
						}
				}
			case Nil => Future.successful(Yield(None))
		}


}
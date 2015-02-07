package io.legs

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.Logger
import io.legs.Coordinator.{JobFailed, JobSuccess}
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.library.{Job, JobType, Queue}
import io.legs.utils.InstructionsFileResolver
import org.slf4j.LoggerFactory

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


class Worker(coordinator: ActorRef, job: Job) extends Actor {

	implicit val ctx = context.dispatcher

	import io.legs.Worker._

	def receive = {
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
		(job.jobType match {
			case JobType.SCHEDULED_CHILD | JobType.AD_HOC  =>
				logger.info(s"cleaning up ${job.jobType} jobId:${job.id} ")
				Queue.removeJob(job)
					.flatMap ( _ => Job.delete(job.id) )
			case ignore => Future.successful(Unit)
		}) andThen {
			case _ =>
				coordinator ! JobSuccess(job.id)
				stop()
		}
	}

	private def workerFail(message:String, e:Option[Throwable] = None){
		if (e.isDefined) logger.error(s"failing jobId:${job.id}  with message:$message",e.get)
		else logger.warn("failing jobId:${job.id}  with message:$message")
		Job.createOrUpdate(job.incRetry)
			.andThen {
				case _ =>
					coordinator ! JobFailed(job.id, message)
					stop()
			}

	}

	private def stop(){
		logger.info(s"stopping worker for jobId:${job.id}")
		context.stop(context.self)
	}


}

object Worker {

	import scala.concurrent.ExecutionContext.Implicits.global

	case class StartWork()

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	def props(coordinator: ActorRef, job:Job) : Props = Props(new Worker(coordinator,job))

	def execute(jsonString: String, state:Map[String,Any] = Map(), userSpecialized : List[Specialization] = Nil): Try[Yield] =
		 Try { Await.result(executeAsync(jsonString, state,userSpecialized),Duration("5 minutes")) }

	def executeAsync(jsonString: String, state:Map[String,Any] = Map(), userSpecialized : List[Specialization] = Nil): Future[Yield] = {

		val steps = Step.from(jsonString)

		try {
			walk(steps, state,userSpecialized)
		} catch {
			case e : TimeoutException => throw new Throwable("time ran out while working",e)
			case e : Exception => throw e
		}

	}

	private case class IterateState(yielded:Map[String,Any],state:Map[String,Any],errOpt:Option[String] = None) {
		lazy val stateAndYield = yielded ++ state
	}

	def walk(steps:List[Step],state:Map[String,Any] = Map(),userSpecialized : List[Specialization] = Nil) : RoutableFuture =
		steps match {
			case x::xs =>
				Specialization.executeStep(x,state,userSpecialized).flatMap {
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
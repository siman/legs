package io.legs

import akka.actor._
import scala.concurrent.duration._
import io.legs.specialized.Queue
import scala.Some
import io.legs.scheduling.Job
import io.legs.Worker.StartWork
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import io.legs.Coordinator.{Stop, JobSuccess, JobFailed, GetStats}
import java.util.logging.{Level, Logger}

class Coordinator(labels: List[String], numMaxWorkers: Int) extends Actor {

	import Coordinator.logger

	var statistics = CoordinatorStatistics()

	private var workers = List.empty[ActorRef]
	private var pollerCancellable : Cancellable = null

	startPollingForWork()

	def receive = {
		case Stop() => stop()
		case GetStats => sender ! statistics
		case JobFailed(jobId, message) =>
			logger.log(Level.SEVERE,s"job failed:$jobId message:$message")
			workers = workers.filter(_ != sender)
			statistics = statistics.failed
			lookForWork()
		case JobSuccess(jobId) =>
			logger.info(s"succeeded jobId:$jobId")
			workers = workers.filter(_ != sender)
			statistics = statistics.succeeded
			println(">>>>>>>>>>>>",statistics)
			lookForWork()
		case unknown => println("coordinator received unknown message",unknown)
	}

	private def stop(){
		logger.info("stopping coordinator...")
		pollerCancellable.cancel()
		context.stop(context.self)
	}

	private def lookForWork(){
		statistics = statistics.touch
		Queue.getNextJobFromQueue(labels) match {
			case Some(job)=>
				logger.info(s"found job id: ${job.id}")
				logger.info(s"current workers count: ${workers.length}")
				spinUpWorker(job)
				if (workers.length < numMaxWorkers) {
					logger.info("polling for more work")
					// look for more work
					lookForWork()
				}
			case None =>
				println("could not find job in the queue... going to sign unemployment")
		}
	}

	private def startPollingForWork(){
		logger.info("startPollingForWork")
		pollerCancellable = context.system.scheduler.schedule(0.seconds,1.seconds){
			if (workers.length < numMaxWorkers) {
				lookForWork()
			}
		}
	}


	private def spinUpWorker(job:Job){
		logger.info(s"spinning up worker with job id:${job.id}")
		val newWorker = context.system.actorOf(Worker.props(context.self,job))
		workers ::= newWorker
		newWorker ! StartWork
	}

}

object Coordinator {

	case class Stop()
	case class GetStats()
	case class JobFailed(jobId: String, message: String)
	case class JobSuccess(jobId:String)

	val actorSystemName  = "LegsCoordinator"

	lazy val logger = Logger.getLogger(this.getClass.getSimpleName)

	def props(labels: List[String], numWorkers: Int) : Props = Props(new Coordinator(labels,numWorkers))

	def start(labels: List[String], numWorkers: Int) : ActorRef = {
		val system = ActorSystem(actorSystemName)
		system.actorOf(props(labels,numWorkers))
	}

}



package io.legs

import akka.actor._
import com.typesafe.scalalogging.Logger
import io.legs.Coordinator.{GetStats, JobFailed, JobSuccess, Stop}
import io.legs.Worker.StartWork
import io.legs.library.{Job, Queue}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Coordinator(labels: List[String], numMaxWorkers: Int,userSpecialized : List[Specialization] = Nil) extends Actor {

	import io.legs.Coordinator.logger

	var statistics = CoordinatorStatistics()

	private var workers = List.empty[ActorRef]
	private var pollerCancellable : Cancellable = null

	startPollingForWork()

	def receive = {
		case Stop() => stop()
		case GetStats => sender ! statistics
		case JobFailed(jobId, message) =>
			logger.error(s"job failed:$jobId message:$message")
			workers = workers.filter(_ != sender)
			statistics = statistics.failed
			lookForWork()
		case JobSuccess(jobId) =>
			logger.info(s"succeeded jobId:$jobId")
			workers = workers.filter(_ != sender)
			statistics = statistics.succeeded
			logger.debug("statistics:" + statistics)
			lookForWork()
		case unknown => logger.error("coordinator received unknown message:" + unknown.toString)
	}

	private def stop(){
		logger.info("stopping coordinator...")
		pollerCancellable.cancel()
		context.stop(context.self)
	}

	private def lookForWork(){
		statistics = statistics.touch
		Queue.getNextJobFromQueue(labels).map {
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
				logger.info("could not find job in the queue... going to sign unemployment")
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
		val newWorker = context.system.actorOf(Worker.props(context.self,job,userSpecialized))
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

	lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	def props(labels: List[String], numWorkers: Int, userSpecialized : List[Specialization] = Nil) : Props = Props(new Coordinator(labels,numWorkers,userSpecialized))

	def start(labels: List[String], numWorkers: Int, userSpecialized : List[Specialization] = Nil) : ActorRef = {
		val system = ActorSystem(actorSystemName)
		system.actorOf(props(labels,numWorkers))
	}

}



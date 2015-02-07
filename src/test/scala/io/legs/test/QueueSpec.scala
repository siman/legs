package io.legs.test

import io.legs.library.{Job, JobType, Priority, Queue}
import io.legs.utils.RedisProvider
import io.legs.{Specialization, Step}
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.{BeforeAndAfter, FunSpec}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

class QueueSpec extends FunSpec with AsyncAssertions with BeforeAndAfter {

	import io.legs.test.TestUtils._

	before {
		RedisProvider.drop("!!!")
	}

	val testJobLabel = "testLabel"

	val testJob = Job(
		"test",List(testJobLabel),Map(),"test job",JobType.AD_HOC,Priority.HIGH,"testtesttest"
	)



	it("runs setup correctly"){
		toBlocking(Queue.setupRedis())
		toBlocking(Job.get(Queue.getSchedulerJob.id)) match {
			case Some(job)=> assertResult(Queue.getSchedulerJob.id) { job.id }
			case None => fail("did not get the job")
		}
	}

	it("gets the next job id"){
		toBlocking(Queue.setupRedis())
		assertResult(Queue.jobsStartValue.toInt + 1 ) { toBlocking(Job.getNextJobId).toInt }
	}

	it("persists a job to redis"){

		toBlocking(Job.createOrUpdate(Queue.getSchedulerJob))

		RedisProvider.blockingRedis[Option[String]] {
			_.hget[String](Job.jobsData_HS, Queue.getSchedulerJob.id)
		} match {
			case None => fail("did not get seeded queue job")
			case Some(jobStr) =>
				assertResult( Queue.getSchedulerJob.id ) { Json.fromJson[Job](Json.parse(jobStr)).get.id }
		}
	}

	it("creates a scheduled job"){
		Job.createOrUpdate(Queue.getSchedulerJob)
		toBlocking(Specialization.executeStep(
			Step("PLAN/when/jobID",None,None),
			Map("when" -> Queue.Plans.oncePerHour, "jobID" -> "100")
		))

		RedisProvider.blockingRedis[Map[String,String]] {
			_.hgetall[String](Queue.schedulePlansKey_HS)
		}.size match {
			case 1 => assertResult(1) { 1 }
			case _ => fail("did not get any items back")
		}

	}

	it("queues specific job") {

		toBlocking(Queue.setupRedis())

		toBlocking(Specialization.executeStep(
			Step("QUEUE/jobID",None,None),
			Map("jobID" -> "100")
		))

		RedisProvider.blockingRedis {
			_.zrange(Queue.queueByLabelKey_ZL(Queue.getSchedulerJob.labels.head),0, -1)
		}.length match {
			case 2 => assertResult(2) { 2 }
			case _ => fail("did not get good result")
		}
	}


	it ("queues all scheduled jobs"){
		toBlocking(Queue.setupRedis())
		toBlocking(Queue.queueAll())

		RedisProvider.blockingRedis{
			_.zrange(Queue.queueByLabelKey_ZL(Queue.getSchedulerJob.labels.head),0, -1)
		}.length match {
			case 2 => // good
			case x => fail("did not get good result,=" + x)
		}
	}

	it("takes the next job in the queue"){
		toBlocking(
			Queue.setupRedis()
				.flatMap(_ => Job.createOrUpdate(testJob))
				.flatMap(_ => Queue.queueJobImmediately(testJob))
		)

		assertResult( true ) { toBlocking(Queue.getNextJobFromQueue(List(testJobLabel))).isDefined }
	}

	it("upon taking a job it adds it to the appropriate working priority zlist and removes it from the label queue"){
		toBlocking(Queue.setupRedis())
		toBlocking(Job.createOrUpdate(testJob))
		toBlocking(Queue.queueJobImmediately(testJob))

		RedisProvider.blockingRedis {
			_.zrange(Queue.queueByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 1 => // good
			case _ => fail("did not get good result")
		}

		RedisProvider.blockingRedis {
			_.zrange(Queue.queueWorkingByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 0 => // good
			case _ => fail("did not get good result")
		}

		Queue.getNextJobFromQueue(List(testJobLabel))

		RedisProvider.blockingRedis {
			_.zrange(Queue.queueByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 0 => // good
			case _ => fail("did not get good result")
		}

		RedisProvider.blockingRedis {
			_.zrange(Queue.queueWorkingByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 1 => //good
			case _ => fail("did not get good result")
		}

	}


	it("deletes a job"){
		toBlocking(Queue.setupRedis())
		toBlocking(Job.createOrUpdate(testJob))
		toBlocking(Queue.queueJobImmediately(testJob))

		val jobOpt = toBlocking(Queue.getNextJobFromQueue(List(testJobLabel)))

		toBlocking(Job.delete(jobOpt.get.id))
		toBlocking(Queue.removeJob(jobOpt.get))

		assertResult(None) {
			RedisProvider.blockingRedis { _.zrank(Queue.queueByLabelKey_ZL(testJobLabel),jobOpt.get.id) }
		}
		assertResult(None) {
			RedisProvider.blockingRedis { _.zrank(Queue.queueDeferredByLabelKey_ZL(testJobLabel),jobOpt.get.id) }
		}
		assertResult(None) {
			RedisProvider.blockingRedis { _.zrank(Queue.queueWorkingByLabelKey_ZL(testJobLabel),jobOpt.get.id) }
		}
		assertResult(None) {
			toBlocking(Job.get(jobOpt.get.id))
		}

		toBlocking(Job.delete(jobOpt.get.id))
		toBlocking(Queue.removeJob(jobOpt.get))

		assertResult(None) { toBlocking(Job.get(Queue.getSchedulerJob.id)) }
		assertResult(None) {
			RedisProvider.blockingRedis { _.hget(Queue.schedulePlansKey_HS,jobOpt.get.id) }
		}

	}

	ignore("updates the last execution date for the parent schedule job"){}
	ignore("takes a job which was too long in the working queue for that label"){}
	ignore("ignores a job which was not long enough in the working queue"){}
	ignore("removes the job from the schedule queue after its done"){}
	ignore("updates the retry count"){}
	ignore("moves the job to ignored after 5 attempts"){}
	ignore("gets the oldest job from the queue"){}
	ignore("retuns normally when there are no jobs in the queue"){}
	ignore("gets job from secondary label"){}
	ignore("updates all relevant labels when scheduling a job"){}
	ignore("updates all the label-queues, score for a job, when it puts job in queue and retries"){}



}

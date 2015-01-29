import io.legs.scheduling.{Priority, JobType, Job}
import io.legs.{Step, Specialization}
import io.legs.specialized.Queue
import io.legs.utils.RedisProvider
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.{BeforeAndAfter, FunSpec}
import play.api.libs.json.Json
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class QueueSpec extends FunSpec with AsyncAssertions with BeforeAndAfter {

	before {
		RedisProvider.drop("!!!")
	}

	val testJobLabel = "testLabel"

	val testJob = Job(
		"test",List(testJobLabel),Map(),"test job",JobType.AD_HOC,Priority.HIGH,"testtesttest"
	)


	it("runs setup correctly"){
		Await.result(Queue.setupRedis(), Duration(2, "seconds"))
		Queue.getJob(Queue.getSchedulerJob.id) match {
			case Some(job)=> assertResult(Queue.getSchedulerJob.id) { job.id }
			case None => fail("did not get the job")
		}
	}

	it("gets the next job id"){
		Await.result(Queue.setupRedis(), Duration(2, "seconds"))
		assertResult( Queue.jobsStartValue.toInt + 1 ) { Queue.getNextJobId.toInt }
	}

	it("persists a job to redis"){

		Await.result(Queue.persistJob(Queue.getSchedulerJob),Duration(2, "seconds"))

		RedisProvider.blocking[Option[String]] {
			_.hget[String](Queue.jobsData_HS, Queue.getSchedulerJob.id)
		} match {
			case None => fail("did not get seeded queue job")
			case Some(jobStr) =>
				assertResult( Queue.getSchedulerJob.id ) { Json.fromJson[Job](Json.parse(jobStr)).get.id }
		}
	}

	it("creates a scheduled job"){
		Queue.persistJob(Queue.getSchedulerJob)
		Await.result(Specialization.executeStep(
			Step("io.legs.specialized.Queue/PLAN/when/jobID",None,None),
			Map("when" -> Queue.Plans.oncePerHour, "jobID" -> "100")
		), Duration("5 seconds"))

		RedisProvider.blocking[Map[String,String]] {
			_.hgetall[String](Queue.schedulePlansKey_HS)
		}.size match {
			case 1 => assertResult(1) { 1 }
			case _ => fail("did not get any items back")
		}

	}

	it("queues specific job") {

		Await.result(Queue.setupRedis(), Duration(2, "seconds"))

		Await.result(Specialization.executeStep(
			Step("io.legs.specialized.Queue/QUEUE/jobID",None,None),
			Map("jobID" -> "100")
		), Duration("5 seconds"))

		RedisProvider.blocking {
			_.zrange(Queue.queueByLabelKey_ZL(Queue.getSchedulerJob.labels.head),0, -1)
		}.length match {
			case 2 => assertResult(2) { 2 }
			case _ => fail("did not get good result")
		}
	}


	it ("queues all scheduled jobs"){
		Await.result(Queue.setupRedis(), Duration(2, "seconds"))
		Queue.queueAll()

		RedisProvider.blocking{
			_.zrange(Queue.queueByLabelKey_ZL(Queue.getSchedulerJob.labels.head),0, -1)
		}.length match {
			case 2 => // good
			case _ => fail("did not get good result")
		}
	}

	it("takes the next job in the queue"){
		Await.result(Queue.setupRedis(), Duration(2, "seconds"))
		Queue.persistJob(testJob)
		Queue.queueJobImmediately(testJob)

		val jobOpt = Queue.getNextJobFromQueue(List(testJobLabel))
		assertResult( true ) { jobOpt.isDefined }
	}

	it("upon taking a job it adds it to the appropriate working priority zlist and removes it from the label queue"){
		Await.result(Queue.setupRedis(), Duration(2, "seconds"))
		Queue.persistJob(testJob)
		Queue.queueJobImmediately(testJob)

		RedisProvider.blocking {
			_.zrange(Queue.queueByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 1 => // good
			case _ => fail("did not get good result")
		}

		RedisProvider.blocking {
			_.zrange(Queue.queueWorkingByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 0 => // good
			case _ => fail("did not get good result")
		}

		Queue.getNextJobFromQueue(List(testJobLabel))

		RedisProvider.blocking {
			_.zrange(Queue.queueByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 0 => // good
			case _ => fail("did not get good result")
		}

		RedisProvider.blocking {
			_.zrange(Queue.queueWorkingByLabelKey_ZL(testJobLabel),0, -1)
		}.length match {
			case 1 => //good
			case _ => fail("did not get good result")
		}

	}


	it("deletes a job"){
		Await.result(Queue.setupRedis(), Duration(2, "seconds"))
		Queue.persistJob(testJob)
		Queue.queueJobImmediately(testJob)

		val jobOpt = Queue.getNextJobFromQueue(List(testJobLabel))

		Queue.deleteJob(jobOpt.get)

		assertResult(None) {
			RedisProvider.blocking { _.zrank(Queue.queueByLabelKey_ZL(testJobLabel),jobOpt.get.id) }
		}
		assertResult(None) {
			RedisProvider.blocking { _.zrank(Queue.queueDeferredByLabelKey_ZL(testJobLabel),jobOpt.get.id) }
		}
		assertResult(None) {
			RedisProvider.blocking { _.zrank(Queue.queueWorkingByLabelKey_ZL(testJobLabel),jobOpt.get.id) }
		}
		assertResult(None) {
			Queue.getJob(jobOpt.get.id)
		}

		Queue.deleteJob(Queue.getSchedulerJob)

		assertResult(None) { Queue.getJob(Queue.getSchedulerJob.id) }
		assertResult(None) {
			RedisProvider.blocking { _.hget(Queue.schedulePlansKey_HS,jobOpt.get.id) }
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

package io.legs.ui.test

import io.legs.library.{Job, Queue}
import io.legs.ui.server.service.Jobs
import io.legs.utils.RedisProvider
import org.scalatest.{BeforeAndAfter, FunSpec}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class ServicesSpec extends FunSpec with BeforeAndAfter {

	import io.legs.test.TestUtils._

	before {
		RedisProvider.drop("!!!")
	}

	describe("jobs"){

		it("gets scheduled jobs"){

			assertResult(Nil){
				Await.result(Jobs.getScheduledJobs(),duration)
			}

			toBlocking(Job.createOrUpdate(Queue.getSchedulerJob))
			toBlocking(Queue.writeJobSchedule(Queue.getSchedulerJob.id,Queue.Plans.oncePerHour))
			val result = toBlocking(Jobs.getScheduledJobs())

			assertResult(1){
				result.length
			}

		}

	}

}
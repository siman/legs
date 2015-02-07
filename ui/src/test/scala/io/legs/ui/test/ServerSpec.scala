package io.legs.ui.test

import io.legs.library.{Job, Queue}
import io.legs.ui.test.server.model.{ScheduledJob, Jobs}
import io.legs.utils.RedisProvider
import org.scalatest.{BeforeAndAfter, FunSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class ServerSpec extends FunSpec with BeforeAndAfter {

	import io.legs.test.TestUtils._

	before {
		RedisProvider.drop("!!!")
	}

	describe("jobs"){

		it("gets scheduled jobs"){

			assertResult(Nil){
				Await.result(Jobs.getScheduledJobs(),duration)
			}

			val results = toBlocking(Future.sequence(List(
				Job.createOrUpdate(Queue.getSchedulerJob),
				Queue.writeJobSchedule(Queue.getSchedulerJob.id,Queue.Plans.oncePerHour),
				Jobs.getScheduledJobs()
			)))

			assertResult(1){
				results.last.asInstanceOf[List[ScheduledJob]].length
			}

		}

	}

}

package io.legs.ui.server.test


import io.legs.library.{Job, Queue}
import io.legs.ui.server.service.JobsService
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
				Await.result(JobsService.read(),duration)
			}

			toBlocking(Job.createOrUpdate(Queue.getSchedulerJob))
			toBlocking(Queue.writeJobSchedule(Queue.getSchedulerJob.id,Queue.Plans.oncePerHour))
			val result = toBlocking(JobsService.read())

			assertResult(1){
				result.length
			}

		}

	}

}

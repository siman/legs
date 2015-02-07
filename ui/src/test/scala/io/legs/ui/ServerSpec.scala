package io.legs.ui

import io.legs.specialized.Queue
import io.legs.ui.server.model.Jobs
import io.legs.utils.RedisProvider
import org.scalatest.{BeforeAndAfter, FunSpec}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class ServerSpec extends FunSpec with BeforeAndAfter {

	val duration = Duration("10 seconds")

	before {
		RedisProvider.drop("!!!")
	}

	describe("jobs"){

		it("gets scheduled jobs"){

			assertResult(Nil){
				Await.result(Jobs.getScheduledJobs(),duration)
			}



		}

	}

}

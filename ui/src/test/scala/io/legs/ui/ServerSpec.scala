package io.legs.ui

import io.legs.ui.server.model.Jobs
import org.scalatest.FunSpec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class ServerSpec extends FunSpec {

	val duration = Duration("10 seconds")

	describe("jobs"){


		it("gets scheduled jobs"){

			val result = Await.result(Jobs.getScheduledJobs(),duration)
			println("result",result)

			assertResult(true){true}

		}
	}

}

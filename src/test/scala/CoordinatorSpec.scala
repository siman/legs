import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestKit
import io.legs.Coordinator.Stop
import io.legs.specialized.Queue
import io.legs.utils.RedisProvider
import io.legs.{Coordinator, CoordinatorStatistics}
import org.scalatest.concurrent.{AsyncAssertions, Eventually}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class CoordinatorSpec extends TestKit(ActorSystem("Step1PrimarySpec")) with FunSpecLike with AsyncAssertions with BeforeAndAfter with Eventually {

	before {
		RedisProvider.drop("!!!")
	}

	it("starts and finds a job for its worker6"){

		Queue.setupRedis()
		Queue.queueAll()

		val coordActorRef = Coordinator.start(List("scheduler"),1)

		eventually(timeout(Span(2,Seconds))){
			implicit val t = akka.util.Timeout(2, TimeUnit.SECONDS) // needed for `?` below
			val f = (coordActorRef ? Coordinator.GetStats).mapTo[CoordinatorStatistics]
			val stats = Await.result(f, Duration("2 seconds"))
			println(stats)
			assertResult(true) { stats.lastWorkQueueCheck.isDefined }
			coordActorRef ! Stop()
		}

	}

}

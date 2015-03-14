

import io.legs.CoordinatorStatistics
import io.legs.library.{Priority, JobType, Job}
import minitest.SimpleTestSuite
import play.api.libs.json.JsValue
import upickle.{read => jsonRead, write => jsonWrite}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object NozzleSpec extends SimpleTestSuite {

	val duration = Duration(2, "seconds")

	def toBlocking[T](future : Future[T]) : T = Await.result(future, duration)

	test("checks upickle"){

		import io.legs.ui.shared.util.PicklingImplicits._
		import io.nozzler.UpickleImplicits._

		val m = CoordinatorStatistics(0,0,0,None) :: CoordinatorStatistics(1,1,1,None) :: Nil
		val json = jsonWrite(m)
		assertResult(m){ jsonRead[List[CoordinatorStatistics]](json) }


		val m2 = Job("instructions","label1" :: "label2" :: Nil,Map.empty[String,JsValue],"description",JobType.AD_HOC,Priority.HIGH,"id")
		val json2 = jsonWrite(m2)
		assertResult(m2){ jsonRead[Job](json2) }

	}

}



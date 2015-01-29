import java.util.UUID

import helpers.{CustomSpecialized, TestSpecializer}
import io.legs.Specialization._
import io.legs.{Worker, Specialization, Step}
import org.scalatest.FunSpec
import org.scalatest.concurrent.AsyncAssertions
import play.api.libs.json.{JsNumber, JsString}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.util.Success

class SpecializationSpec extends FunSpec with AsyncAssertions {

	it("intercepts routes with a parameter"){
		val uuid = UUID.randomUUID().toString
		Specialization.executeStep(Step("helpers.TestSpecializer/incr/key", Some(Map("key" -> JsString(uuid))),None), Map())
		Specialization.executeStep(Step("helpers.TestSpecializer/incr/key", Some(Map("key" -> JsString(uuid))),None), Map())
		Specialization.executeStep(Step("helpers.TestSpecializer/incr/key", Some(Map("key" -> JsString(uuid))),None), Map())
		Specialization.executeStep(Step("helpers.TestSpecializer/incr/key", Some(Map("key" -> JsString(uuid))),None), Map()).onFailure {
			case (e)=> println("<<<<>>>",e)
		}
		assertResult(4) { TestSpecializer.getKeyValue(uuid) }
	}

	it("yields"){
		val res = Await.result(Specialization.executeStep(
			Step("helpers.TestSpecializer/incrState/from",None,Some("ztarget")),
			Map("from" -> 20)
		), Duration("5 seconds"))

		assertResult( Yield(Some(21)) ) { res }

	}

	it ("does not yield when not specified"){
		val res = Await.result(Specialization.executeStep(
			Step("helpers.TestSpecializer/incrState/from",None,None),
			Map("from" -> 20)
		), Duration("5 seconds"))

		assertResult( Yield(Some(21)) ) { res }

	}

	it("allows routes with just method names instead of full package path"){
		Await.result(Specialization.executeStep(
			Step("GENERATE/start/end",None,None),
			Map("start"-> JsNumber(20),"end"->JsNumber(22))
		),Duration("5 seconds")) match {
			case Yield(result) =>
				assertResult(result){ Some(List(20, 21)) }
			case bad =>
				fail("bad result")
		}
	}



	it("supports custom specializations provided implicitly in scope"){

		val testValue = "thisandthat"
		val customJson =
			s"""
			  |[{
			  |	"action":"CUSTOM_THING/$${\\"$testValue\\"}"
			  |}]
			""".stripMargin

		assertResult(Success(Yield(Some(testValue + "!")))){
			Worker.execute(customJson,Map.empty[String,Any],CustomSpecialized :: Nil)
		}

	}

}

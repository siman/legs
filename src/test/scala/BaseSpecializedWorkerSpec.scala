
import helpers.TestSpecializer
import io.legs.specialized.ToolsSpecialized
import org.scalatest.FunSpec
import org.scalatest.concurrent.{AsyncAssertions, Eventually}
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json.{JsArray, JsString, Json}

import scala.concurrent.ExecutionContext.Implicits.global


class BaseSpecializedWorkerSpec extends FunSpec with AsyncAssertions with Eventually {


	it("needs to support MAP_PAR operation"){

		val instructions = s"""[{"action":"helpers.TestSpecializer/incr/aParam"}]"""

		ToolsSpecialized.invokeAction("MAP_PAR", List("over", "toParam", "instructions"), Map(),
			Map("over" -> JsArray((1 to 10).map(_.toString).map(JsString).toSeq), "toParam" -> JsString("aParam"), "instructions" -> Json.parse(instructions)))


		eventually(timeout(Span(5,Seconds))) {
			assertResult((1 to 10).map(_=>1)) {
				(1 to 10).map(_.toString).map(TestSpecializer.getKeyValue)
			}
		}
	}

	ignore("needs to support ITERATE blocking operation"){

	}

}

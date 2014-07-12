import helpers.TestSpecializer
import io.legs.specialized.Tools
import java.util.UUID
import org.scalatest.FunSpec
import play.api.libs.json.{JsString, JsArray, Json}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created: 6/7/14 11:47 AM
 */
class ToolsSpec extends FunSpec  {

	it("loops over a set of instructions, until.."){

		val key = UUID.randomUUID().toString

		val check =
			s"""
			  |[{
			  |	"action":"helpers.TestSpecializer/get/key",
			  | "values" : {
			  | 	"key" : "$key"
			  | },
			  | "yields" : "value"
			  |},{
			  |"action":"IS_STRING_DIFFERENT/value/compare",
			  |"values":{
			  | "compare":"5"
			  |}
			  |}]
			""".stripMargin

		val loop =
			s"""
			   |[{
			   |	"action":"helpers.TestSpecializer/incr/key",
			   |	"values": {
			   | 		"key":"$key"
			   |	}
			   |}]
			 """.stripMargin

		Await.result(
			Tools.LOOP_WHILE(
				Map(),
				Json.parse(check).as[JsArray],
				Json.parse(loop).as[JsArray]),
			Duration("5 seconds")
		)

		assertResult(5) { TestSpecializer.getKeyValue(key) }
	}

	it("checks for mandatory values and fails when one is missing"){
		assertResult(true) {
			try {
				Await.result(Tools.VERIFY_VALUES(Map("item1" -> 1,"item2" -> 2,"item3" -> 3),List(JsString("item1"),JsString("item2"),JsString("item3"),JsString("missing"))),Duration("5 seconds"))
				false
			} catch {
				case e : Throwable => true
			}
		}
	}

	ignore("checks for mandatory values and succeeds when all are present"){
		assertResult(true) {
			Await.result(Tools.VERIFY_VALUES(Map("item1" -> 1,"item2" -> 2,"item3" -> 3),List(JsString("item1"),JsString("item2"),JsString("item3"))),Duration("5 seconds")).valueOpt.isDefined
		}
	}

}

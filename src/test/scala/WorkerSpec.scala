import helpers.TestSpecializer
import java.util.UUID
import org.scalatest.concurrent.{Eventually, AsyncAssertions}
import org.scalatest.FunSpec

import io.legs.Worker
import scala.util.{Failure, Success}

class WorkerSpec extends FunSpec with AsyncAssertions with Eventually {

	def getUUID = UUID.randomUUID().toString

	it("accepts json job string"){

		val uuid = getUUID

		val instructions= s"""
			  |[
			  |	{
			  | 	"action":"io.legs.specialized.Tools/MAP_PAR/overP/toP/instructionsP",
			  |  	"values": {
			  |   		"overP": [1,2,3,4,5,6,7,8,9,10],
			  |     	"toP": "num",
			  |      	"instructionsP":[
			  |       		{
			  |         		"action":"helpers.TestSpecializer/incr/key",
			  |           		"values":{
			  |             		"key":"$uuid"
			  |               	}
			  |         	}
			  |       	]
			  |     }
			  |	}
			  |]
			""".stripMargin


		Worker.execute(instructions) match {
			case Success(_)=>
				assertResult(10) { TestSpecializer.getKeyValue(uuid) }
			case Failure(e)=> fail(e)
		}

	}

	it("passes on yielded value"){
		val instructions =
			"""
			  |[{
			  |	"action": "helpers.TestSpecializer/echo/takeP",
			  |	"values": {	"takeP":"blah" },
			  |	"yields":"kuku"
			  |},{
			  |	"action": "helpers.TestSpecializer/echo/kuku"
			  |}]
			""".stripMargin
		Worker.execute(instructions,Map("blah"->"zzz")) match {
			case Success(v)=>
				assertResult(Some("blah")) { v.valueOpt }
			case Failure(e)=> fail(e)
		}
	}

	it("further transforms a yielded value (when applicable)"){
		val instructions =
		"""
		  |[{"action" : "helpers.TestSpecializer/echo/someValue",
		  | "transform" : [
		  |  { "action" : "TRIM/$v" }
		  | ],
		  | "yields" : "output"
		  |}
		  |]
		""".stripMargin

		Worker.execute(instructions, Map("someValue" -> " result ")) match {
			case Success(v)=> assertResult(Some("result")) { v.valueOpt }
			case Failure(e)=> fail (e)
		}

	}


}

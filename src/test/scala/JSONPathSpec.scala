import io.legs.Specialization.Yield
import io.legs.specialized.JsonSpecialized
import org.scalatest.FunSpec

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class JSONPathSpec extends FunSpec {



	it("finds all the nodes"){
		val f = JsonSpecialized.invokeAction("JSONPATH",List("json","query"),Map(
			"json" -> json,
			"query" -> "$.store.book[*].author"
		),Map())

		assertResult(Yield(Some(List("Nigel Rees","Evelyn Waugh","Herman Melville","J. R. R. Tolkien")))){
			val res = Await.result(f, Duration("10 second"))
//			println(res.valueOpt.get.asInstanceOf[List[Any]].head.getClass)
			res
		}
	}

	val json =
		"""
		  |{
		  |    "store": {
		  |        "book": [
		  |            {
		  |                "category": "reference",
		  |                "author": "Nigel Rees",
		  |                "title": "Sayings of the Century",
		  |                "price": 8.95
		  |            },
		  |            {
		  |                "category": "fiction",
		  |                "author": "Evelyn Waugh",
		  |                "title": "Sword of Honour",
		  |                "price": 12.99
		  |            },
		  |            {
		  |                "category": "fiction",
		  |                "author": "Herman Melville",
		  |                "title": "Moby Dick",
		  |                "isbn": "0-553-21311-3",
		  |                "price": 8.99
		  |            },
		  |            {
		  |                "category": "fiction",
		  |                "author": "J. R. R. Tolkien",
		  |                "title": "The Lord of the Rings",
		  |                "isbn": "0-395-19395-8",
		  |                "price": 22.99
		  |            }
		  |        ],
		  |        "bicycle": {
		  |            "color": "red",
		  |            "price": 19.95
		  |        }
		  |    },
		  |    "expensive": 10
		  |}
		""".stripMargin
}

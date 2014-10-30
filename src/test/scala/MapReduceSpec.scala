import io.legs.Specialization.Yield
import io.legs.specialized.MapReduce
import org.scalatest.FunSpec
import play.api.libs.json.JsString

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created: 6/3/14 8:22 PM
 */
class MapReduceSpec extends FunSpec {

	it("evaluates a map reduce expression while operating on a list of strings"){

		val collection = List("one","two","three","four")
		val map = JsString(
			"""
			  |function map(item, collection, emitter){
			  | emitter.emit("items",item);
			  | emitter.emit("counts", collection.length());
			  |}
			""".stripMargin
		)

		val reduce = JsString(
			"""
			  |var reduce = function(key, values){
			  | if (key == "items") {
			  |   return values.mkString(",");
			  | } else {
			  |   return values.length();
			  | }
			  |}
			""".stripMargin
		)

		val f = MapReduce.invokeAction("MAP_REDUCE",List("collection","map","reduce"),Map("collection" -> collection)
			,Map("map" -> map, "reduce" -> reduce))

		assertResult(Yield(Some(Map("items" -> "four,three,two,one", "counts" -> 4)))){
			Await.result(f, Duration("10 second"))
		}


	}

}

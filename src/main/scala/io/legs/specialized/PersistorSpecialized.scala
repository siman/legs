package io.legs.specialized

import java.io.FileWriter

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsParamAnnotation, LegsFunctionAnnotation}
import play.api.libs.json._

import scala.concurrent._

object PersistorSpecialized extends Specialization {

	object WriteSyncObj

	@LegsFunctionAnnotation(
		details = "persist content to file",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def TO_FILE(state: Specialization.State,
		keys : List[JsString] @LegsParamAnnotation("list of keys to be extracted from state to be serialized"),
		filePath: String @LegsParamAnnotation("the full file path to be used for persisting the contents")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			var appending = List.empty[String]
			keys.foreach(k=> state.get(k.value) match {
				case Some(v : List[Any]) => v.foreach( _v=> appending ::= _v.toString + "\n"  )
				case Some(v : String) => appending ::= v
				case Some(v : Integer) => appending ::=  v.toString
				case ignore =>
			})

			if (appending.nonEmpty){
				// file appending needs to be globally locked
				WriteSyncObj.synchronized {
					val writer = new FileWriter(filePath,true)
					writer.write(appending.mkString.toCharArray)
					writer.close()
				}
			}
			Yield(None)
		}

	@LegsFunctionAnnotation(
		details = "persist state key contents to file as a json object",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def TO_FILE_AS_JSON(state: Specialization.State,
		keys : List[JsString] @LegsParamAnnotation("list of keys to be extracted from state to be serialized"),
		filePath: String @LegsParamAnnotation("the full file path to be used for persisting the contents")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			WriteSyncObj.synchronized {
				val writer = new FileWriter(filePath,true)
				val objStr = ToolsSpecialized.constructJson(state, keys)
				writer.write(objStr)
				writer.close()
			}

			Yield(None)

		}


}

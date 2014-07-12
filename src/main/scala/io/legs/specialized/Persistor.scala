package io.legs.specialized

import java.io.FileWriter

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import play.api.libs.json._

import scala.concurrent._

object Persistor extends Specialization {

	object WriteSyncObj
  
	def TO_FILE(state: Specialization.State, keys : List[JsString], filePath: String)(implicit ctx : ExecutionContext) : RoutableFuture =
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

	def TO_FILE_AS_JSON(state: Specialization.State, keys : List[JsString], filePath: String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {

			val out = keys.foldLeft(Map.empty[String,JsValue]) { (_out,key)=>
				state.get(key.value) match {
					case Some(v: List[Any]) =>
						_out + (key.value -> JsArray(
							v.map {
								case x : Int => JsNumber(BigDecimal(x))
								case x : String => JsString(x)
								case x => JsString(x.toString)
							}
						))
					case Some(v: String) => _out + (key.value -> JsString(v))
					case Some(v: Integer) => _out + (key.value -> JsNumber(BigDecimal(v)))
					case ignore => _out
				}
			}

			if (out.nonEmpty){
				WriteSyncObj.synchronized {
					val writer = new FileWriter(filePath,true)
					val objStr = Json.toJson(out).toString()
					writer.write(objStr)
					writer.close()
				}
			}
			Yield(None)

		}


}

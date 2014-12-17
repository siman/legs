package io.legs.specialized

import java.util.logging.Logger

import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import io.legs.Specialization.Yield
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsNumber

/**
 * Created: 6/11/13 4:53 PM
 */
object Tools extends Specialization {

	val specializedBaseLogger = Logger.getLogger(this.getClass.getSimpleName)

	def CAST(state: Specialization.State, input: Any, toType:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		input match {
			case _ : Int => toType match {
				case "String" => Future.successful(Yield(Some(input.toString)))
			}
			case default => Future.successful(Yield(Some(input)))
		}

	def DEBUG(state:Specialization.State) : RoutableFuture = {
		specializedBaseLogger.info("START dumping DEBUG information for state")
		state.keys.map(k=> { specializedBaseLogger.info(s"key:'$k' value:'${state.get(k).head}' ") } )
		specializedBaseLogger.info("FINISHED dumping DEBUG information for state")
		Future.successful(Yield(None))
	}


	private case class IterateState(yielded:Map[String,Any],state:Map[String,Any],errOpt:Option[String] = None) {
		lazy val stateAndYield = yielded ++ state
	}

	def IS_STRINGS_EQUAL(state:Specialization.State, val1 :Any, val2:Any) : RoutableFuture =
		Future.successful(Yield(Some(val1.toString == val2.toString)))

	def IS_STRING_DIFFERENT(state:Specialization.State, val1 :Any, val2:Any) : RoutableFuture =
		Future.successful(Yield(Some(val1.toString != val2.toString)))


	def IF(state:Specialization.State, value:Any, trueInstructions: JsArray, falseInstructions: JsArray) : RoutableFuture =
		value match {
			case true | "true" | "True" =>
				val steps = Step.from(trueInstructions)
				Worker.walk(steps,state)
			case _ =>
				val steps = Step.from(falseInstructions)
				Worker.walk(steps,state)
		}


	def MAP_PAR(state:Specialization.State, over: List[Any], toValueName: String, furtherInstructions: JsArray)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future.fold(
			over.map { o => Worker.walk(Step.from(furtherInstructions), state + (toValueName -> o)) }
		)(Yield(Some(Nil)) : Yield) {
			(out, result) =>
				result match {
					case Yield(Some(yielded)) =>
						val prevYield = out.valueOpt.getOrElse(List.empty[Any]).asInstanceOf[List[Any]]
						Yield(Some( yielded::prevYield ))
					case othewise => othewise
				}
		}

	def GET_MAP_KEY(state:Specialization.State, map : Map[String,Any], key : String) : RoutableFuture =
		map.contains(key) match {
			case true => Future.successful(Yield(Some(map(key))))
			case false => Future.failed(new Throwable(s"could not find key:$key in map"))
		}

	// todo add a limit to number of iteratios (as parameter?)
	def LOOP_WHILE(state:Specialization.State, checkInstructions : JsArray, overInstructions : JsArray)(implicit ctx : ExecutionContext) : RoutableFuture =
		Worker.walk(Step.from(checkInstructions),state).flatMap {
			case Yield(Some(true))=>
				Worker.walk(Step.from(overInstructions),state).flatMap {
					_ => LOOP_WHILE(state, checkInstructions, overInstructions)
				}

			case Yield(ignored)=>
				Future.successful(Yield(None))
		}

	def ECHO(state:Specialization.State,value:Any) : RoutableFuture = {
		println(value)
		Future.successful(Yield(Some(value)))
	}

	def VERIFY_VALUES(state:Specialization.State, values : List[JsString]) : RoutableFuture =
		values.map(_.value).forall(state.keys.toList.contains) match {
			case true => Future.successful(Yield(None))
			case false => Future.failed(new Throwable("could not verify all values, missing: " + values.filterNot(state.keys.toList.contains).mkString(",") ))
		}

	def AS_JSON(state:Specialization.State, keys : List[JsString])(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Yield(Some(constructJson(state,keys)))
		}

	def constructJson(state:Specialization.State, keys : List[JsString]) : String =
		Json.toJson(keys.foldLeft(Map.empty[String, JsValue]) {
			(_out, key) =>
				state.get(key.value) match {
					case Some(v: List[Any]) =>
						_out + (key.value -> JsArray(
							v.map {
								case x: Int => JsNumber(BigDecimal(x))
								case x: String => JsString(x)
								case x => JsString(x.toString)
							}
						))
					case Some(v: String) => _out + (key.value -> JsString(v))
					case Some(v: Integer) => _out + (key.value -> JsNumber(BigDecimal(v)))
					case ignore => _out
				}
		}).toString()


}

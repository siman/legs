package io.legs.specialized

import io.legs._
import play.api.libs.json.{JsString, JsArray}
import scala.util.{Try, Failure, Success}
import scala.concurrent.{ExecutionContext, Future}
import java.util.logging.Logger
import io.legs.Specialization.{Yield, RoutableFuture}

/**
 * Created: 6/11/13 4:53 PM
 */
object Tools extends Specialization {

	val specializedBaseLogger = Logger.getLogger(this.getClass.getSimpleName)

	def DEBUG(state:Specialization.State) : RoutableFuture = {
		specializedBaseLogger.info("START dumping DEBUG information for state")
		state.keys.map(k=> { specializedBaseLogger.info(s"key:'$k' value:'${state.get(k).head}' ") } )
		specializedBaseLogger.info("FINISHED dumping DEBUG information for state")
		Future.successful(Success(Yield(None)))
	}


	private case class IterateState(yielded:Map[String,Any],state:Map[String,Any],errOpt:Option[String] = None) {
		lazy val stateAndYield = yielded ++ state
	}

	def IS_STRINGS_EQUAL(state:Specialization.State, val1 :Any, val2:Any) : RoutableFuture =
		Future.successful(Success(Yield(Some(val1.toString == val2.toString))))

	def IS_STRING_DIFFERENT(state:Specialization.State, val1 :Any, val2:Any) : RoutableFuture =
		Future.successful(Success(Yield(Some(val1.toString != val2.toString))))


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
		)(Success(Yield(Some(Nil))) : Try[Yield]) {
			(out, result) =>
				if (out.isFailure) out
				else result match {
					case Success(Yield(Some(yielded)))=>
						val prevYield = out.get.valueOpt.getOrElse(List.empty[Any]).asInstanceOf[List[Any]]
						Success(Yield(Some( yielded::prevYield )))
					case othewise => othewise
				}
		}

	def GET_MAP_KEY(state:Specialization.State, map : Map[String,Any], key : String) : RoutableFuture =
		map.contains(key) match {
			case true => Future.successful(Success(Yield(Some(map(key)))))
			case false => Future.failed(new Throwable(s"could not find key:$key in map"))
		}


	// todo add a limit to number of iteratios (as parameter?)
	def LOOP_WHILE(state:Specialization.State, checkInstructions : JsArray, overInstructions : JsArray)(implicit ctx : ExecutionContext) : RoutableFuture =
		Worker.walk(Step.from(checkInstructions),state).flatMap {
			case Success(Yield(Some(true)))=>
				Worker.walk(Step.from(overInstructions),state).flatMap {
					case Success(_) => LOOP_WHILE(state, checkInstructions, overInstructions)
					case Failure(e) => Future.failed(e)
				}
			case Success(Yield(ignored))=>
				Future.successful(Success(Yield(None)))
			case Failure(e) =>
				Future.failed(e)
		}


	def ECHO(state:Specialization.State,value:Any) : RoutableFuture = {
		println(value)
		Future.successful(Success(Yield(Some(value))))
	}

	def VERIFY_VALUES(state:Specialization.State, values : List[JsString]) : RoutableFuture =
		values.map(_.value).forall(state.keys.toList.contains) match {
			case true => Future.successful(Success(Yield(None)))
			case false => Future.failed(new Throwable("could not verify all values, missing: " + values.filterNot(state.keys.toList.contains).mkString(",") ))
		}


}

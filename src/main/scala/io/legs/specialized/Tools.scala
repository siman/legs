package io.legs.specialized

import java.util.logging.Logger

import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs._
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import play.api.libs.json.{JsArray, JsNumber, JsString, _}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created: 6/11/13 4:53 PM
 */
object Tools extends Specialization {

	val specializedBaseLogger = Logger.getLogger(this.getClass.getSimpleName)


	@LegsFunctionAnnotation(
		details = "convert given input value to Int/String",
		yieldType = "Int/String",
		yieldDetails = "the type as provided"
	)
	def CAST(state: Specialization.State,
		input: Any @LegsParamAnnotation("either String/Int"),
		toType:String @LegsParamAnnotation("can be either Int/String")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		input match {
			case in : Int 		if toType == "String" => 	Future.successful(Yield(Some(in.toString)))
			case in : String 	if toType == "Int" => 		Future.successful(Yield(Some(in.toInt)))
			case default => 								Future.successful(Yield(Some(input)))
		}

	@LegsFunctionAnnotation(
		details = "helper function to debug (print to STDOUT) the state",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def DEBUG(state:Specialization.State) : RoutableFuture = {
		specializedBaseLogger.info("START dumping DEBUG information for state")
		state.keys.map(k=> { specializedBaseLogger.info(s"key:'$k' value:'${state.get(k).head}' ") } )
		specializedBaseLogger.info("FINISHED dumping DEBUG information for state")
		Future.successful(Yield(None))
	}


	private case class IterateState(yielded:Map[String,Any],state:Map[String,Any],errOpt:Option[String] = None) {
		lazy val stateAndYield = yielded ++ state
	}

	@LegsFunctionAnnotation(
		details = "check if two inputs stringified are equal",
		yieldType = Boolean,
		yieldDetails = "true of they are, false otherwise"
	)
	def IS_STRINGS_EQUAL(state:Specialization.State,
		left : Any @LegsParamAnnotation("left value"),
		right : Any @LegsParamAnnotation("right value")
	) : RoutableFuture =
		Future.successful(Yield(Some(left.toString == right.toString)))

	@LegsFunctionAnnotation(
		details = "check if two inputs stringified are different",
		yieldType = Boolean,
		yieldDetails = "true if different, false otherwise"
	)
	def IS_STRING_DIFFERENT(state:Specialization.State,
		left :Any @LegsParamAnnotation("left value"),
		right : Any @LegsParamAnnotation("right value")
	) : RoutableFuture =
		Future.successful(Yield(Some(left.toString != right.toString)))

	@LegsFunctionAnnotation(
		details = "evaluates input value as boolean string true or 1, otherwise false. executes relevant block ",
		yieldType = AnyRef,
		yieldDetails = "what ever the evaluated block is returning"
	)
	def IF(state:Specialization.State,
		value : Any @LegsParamAnnotation("Boolean input value"),
		trueInstructions: JsArray @LegsParamAnnotation("instrucitons block to be executed when true"),
		falseInstructions: JsArray @LegsParamAnnotation("instrucitons block to be executed when false")
	) : RoutableFuture =
		value.toString.toLowerCase match {
			case "true" | "1" =>
				val steps = Step.from(trueInstructions)
				Worker.walk(steps,state)
			case _ =>
				val steps = Step.from(falseInstructions)
				Worker.walk(steps,state)
		}

	@LegsFunctionAnnotation(
		details = "iterate over a collection of values and produce a new resulting list from applying further instructions on each input vlaue",
		yieldType = List.empty[Any],
		yieldDetails = "list of resulting transformed values"
	)
	def MAP_PAR(state:Specialization.State,
		inputList: List[Any] @LegsParamAnnotation("input list of values"),
		toValueName: String @LegsParamAnnotation("each item is assigned this name when iterating"),
		furtherInstructions: JsArray @LegsParamAnnotation("instructions set to use when manipulating a single value")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future.fold(
			inputList.map { o => Worker.walk(Step.from(furtherInstructions), state + (toValueName -> o)) }
		)(Yield(Some(Nil)) : Yield) {
			(out, result) =>
				result match {
					case Yield(Some(yielded)) =>
						val prevYield = out.valueOpt.getOrElse(List.empty[Any]).asInstanceOf[List[Any]]
						Yield(Some( yielded::prevYield ))
					case othewise => othewise
				}
		}

	@LegsFunctionAnnotation(
		details = "get a single entry form a map by given key",
		yieldType = AnyRef,
		yieldDetails = "value produced by key from the map"
	)
	def GET_MAP_KEY(state:Specialization.State,
		map : Map[String,Any] @LegsParamAnnotation("a map data structure to query"),
		key : String @LegsParamAnnotation("key to use for querying the map")
	) : RoutableFuture =
		map.contains(key) match {
			case true => Future.successful(Yield(Some(map(key))))
			case false => Future.failed(new Throwable(s"could not find key:$key in map"))
		}

	// todo add a limit to number of iteratios (as parameter?)
	@LegsFunctionAnnotation(
		details = "loop while given instructions yield true",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def LOOP_WHILE(state:Specialization.State,
		checkInstructions : JsArray @LegsParamAnnotation("instructions to evaluate for stop condition - should yield boolean"),
		overInstructions : JsArray @LegsParamAnnotation("perform instructions inside the while loop if the checkInstructions yielded true")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Worker.walk(Step.from(checkInstructions),state).flatMap {
			case Yield(Some(true))=>
				Worker.walk(Step.from(overInstructions),state).flatMap {
					_ => LOOP_WHILE(state, checkInstructions, overInstructions)
				}

			case Yield(ignored)=>
				Future.successful(Yield(None))
		}

	@LegsFunctionAnnotation(
		details = "yield and print (STDOUT) some given value",
		yieldType = AnyRef,
		yieldDetails = "state value"
	)
	def ECHO(state:Specialization.State,
		value:Any @LegsParamAnnotation("some provided value")
	) : RoutableFuture = {
		println(value)
		Future.successful(Yield(Some(value)))
	}

	@LegsFunctionAnnotation(
		details = "check that all provided keys are defined in state, otherwise fail the job",
		yieldType = None,
		yieldDetails = "nothing is yielded"
	)
	def VERIFY_VALUES(state:Specialization.State,
		keys : List[JsString] @LegsParamAnnotation("list of keys to check for")
	) : RoutableFuture =
		keys.map(_.value).forall(state.keys.toList.contains) match {
			case true => Future.successful(Yield(None))
			case false => Future.failed(new Throwable("could not verify all values, missing: " + keys.filterNot(state.keys.toList.contains).mkString(",") ))
		}

	@LegsFunctionAnnotation(
		details = "take state values and put them into a JSON map",
		yieldType = "String",
		yieldDetails = "string representation of JSON value put togather"
	)
	def AS_JSON(state:Specialization.State,
		keys : List[JsString] @LegsParamAnnotation("list of keys to take from state")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Yield(Some(constructJson(state,keys)))
		}

	def constructJson(state:Specialization.State,keys : List[JsString] @LegsParamAnnotation("")) : String =
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

package io.legs.specialized

import javax.script.{Invocable, ScriptEngineManager}

import com.typesafe.scalalogging.Logger
import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import org.slf4j.LoggerFactory

import scala.concurrent._

object JsEngineSpecialized extends Specialization {

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	type EmitMap = scala.collection.mutable.HashMap[String,List[Any]]

	@LegsFunctionAnnotation(
		details = "execute a JavaScript Map and Reduce functions over input collection",
		yieldType = Map.empty[String,Any],
		yieldDetails = "indices and values after map reduce"
	)
	def MAP_REDUCE(state: Specialization.State,
		collection : List[Any] @LegsParamAnnotation("list of input values"),
		map : String @LegsParamAnnotation("needs to contain \"function map(item, collection,emitter) {...} \" " +
			"call `emitter.emit(key,valie)` to emit values "),
		reduce : String @LegsParamAnnotation("needs to contain \"function reduce(key, values){ ... }\" " +
			"returned value is reduces to the resulting map for that key")
	)(implicit ctx : ExecutionContext) : RoutableFuture  =
		Future {

			val mapperEngine = new ScriptEngineManager(null).getEngineByName("nashorn")
			val mapper = mapperEngine.asInstanceOf[Invocable]

			val reducerEngine = new ScriptEngineManager(null).getEngineByName("nashorn")
			val reducer = reducerEngine.asInstanceOf[Invocable]

			try {
				mapperEngine.eval(map)
				reducerEngine.eval(reduce)

				val mapped = collection.foldLeft(Map.empty[String,List[Any]]) {
					(out, item) =>
						val _em = new Emitter()
						mapper.invokeFunction("map",item.asInstanceOf[AnyRef],collection,_em)
						_em.combine(out)
				}

				val reduced = mapped.map {
					case ((k,values)) =>
						(k,reducer.invokeFunction("reduce",k,values))
				}
				Yield(Some(reduced))
			} catch {
				case e : Throwable =>
					logger.error("error while running map reduce",e)
					throw e
			}
		}


	case class Emitter(
		emitted : scala.collection.mutable.ListBuffer[(String,Any)] = new scala.collection.mutable.ListBuffer[(String,Any)]()
	) {
		def emit(key: String,value : Any) = emitted.append((key,value))
		def combine(another : Map[String,List[Any]]) : Map[String,List[Any]] = emitted.foldLeft(another) {
			(out, kv) =>
				out + (kv._1 -> (kv._2::out.getOrElse(kv._1,List.empty[Any])))
		}
	}

	@LegsFunctionAnnotation(
		details = "execute arbitrary JavaScript code on some provided input",
		yieldType = Map.empty[String,Any],
		yieldDetails = "indices and values after map reduce"
	)
	def EXECUTE(state: Specialization.State,
		input : AnyRef @LegsParamAnnotation("some input value"),
		executor : String @LegsParamAnnotation("executor function for which the first parameter is the input value")
	)(implicit ctx : ExecutionContext) : RoutableFuture  =
		Future {
			val mapperEngine = new ScriptEngineManager(null).getEngineByName("nashorn")
			val mapper = mapperEngine.asInstanceOf[Invocable]

			mapperEngine.eval(executor)
			Yield(Some(mapper.invokeFunction("executor",input)))
		}




}

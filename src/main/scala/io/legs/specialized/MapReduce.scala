package io.legs.specialized

import io.legs.Specialization
import javax.script.{Invocable, ScriptEngineManager}
import scala.util.{Failure, Success}
import scala.concurrent._
import java.util.logging.{Level, Logger}
import io.legs.Specialization.{Yield, RoutableFuture}

object MapReduce extends Specialization {

	private lazy val logger = Logger.getLogger(this.getClass.getSimpleName)

	type EmitMap = scala.collection.mutable.HashMap[String,List[Any]]

	def MAP_REDUCE(state: Specialization.State, collection : List[Any], map : String, reduce : String)(implicit ctx : ExecutionContext) : RoutableFuture  =
		future {

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

				Success(Yield(Some(reduced)))
			} catch {
				case e : Throwable =>
					logger.log(Level.SEVERE,"error while running map reduce",e)
					Failure(e)
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



}

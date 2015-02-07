package io.legs

import java.util.UUID

import com.typesafe.scalalogging.Logger
import io.legs.specialized._
import io.legs.utils.JsonFriend
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}


trait Specialization {

	import io.legs.Specialization._

	private lazy val spcializationLogger = Logger(LoggerFactory.getLogger(getClass))

	private def routes =
		this.getClass.getMethods.toList.filter(_.getGenericReturnType == compareType).map(m=> (m.getName,m.getParameterTypes,m ) )

	private def getRoute(name:String, numArgs:Int) =
		// need to add 1 to count since the first parameter is state, which should always be there
		// need to subtract 1 to count since the implicit context
		routes.find( r=> r._1 == name && (r._2.length - (if (r._2.last == classOf[ExecutionContext]) 1 else 0 )) == numArgs +1 )

	def invokeAction(name: String, paramNames: List[String], state: Specialization.State, actionValues:Map[String,JsValue])(implicit ctx : ExecutionContext) : RoutableFuture = {
		spcializationLogger.info(s"invoking action:`$name`")
		getRoute(name, paramNames.length) match {
			case Some(route) =>
				val paramResolver = actionValues ++ state
				if (Specialization.allParamsDefined(paramNames, paramResolver)){
					// drop left state parameter and execution context if defined
					val expectedTypeList =
						if (route._2.last == classOf[ExecutionContext])
							route._2.drop(1).dropRight(1)
						else
							route._2.drop(1)
					val resolvedArgs = expectedTypeList.zip(paramNames).foldRight(List[Any]()) {(expType,args)=>
						expType._1.getInterfaces.contains(classOf[JsValue]) match {
							case true=> paramResolver.get(expType._2).get::args
							case false=> JsonFriend.materialize(paramResolver.get(expType._2).get)::args
						}
					}
					val params =
						if (route._2.last == classOf[ExecutionContext])
							state :: resolvedArgs ::: List(ctx)
						else
							state :: resolvedArgs
					try {
						route._3.invoke(this, params.toSeq.asInstanceOf[Seq[Object]] : _* ).asInstanceOf[RoutableFuture]
					} catch {
						case e:Exception =>
							spcializationLogger.error(s"failing invocation of:$name",e)
							spcializationLogger.debug("resolved params" + params)
							Future.failed(new Throwable(s"failing invocation of:$name",e))

					}
				} else {
					val msg = s"could not locate some parameters in state:${Specialization.getMissingParameters(paramNames, paramResolver).mkString(",")}"
					spcializationLogger.error(msg)
					Future.failed(new Throwable(msg))
				}

			case None =>
				spcializationLogger.error(s"could not find method $name")
				Future.failed(new Throwable(s"could not find method $name"))
		}

	}
}

object Specialization {

	case class Yield(valueOpt:Option[Any])

	type State = Map[String,Any]
	type RoutableFuture = Future[Yield]

	implicit val pool = ExecutionContext.fromExecutor(new scala.concurrent.forkjoin.ForkJoinPool)

	val tenSecDuration = Duration(10, "second")
	val oneMinuteDuration = Duration(60, "second")

	implicit val waitFor = Duration(60, "second")

	lazy val registeredSpecializedClasses = List(LinkTrackerSpecialized,JsonSpecialized,NumbersSpecialized,PersistorSpecialized,QueueSpecialized,SimpleScraperSpecialized,StringsSpecialized,ToolsSpecialized,WebDriverSpecialized,JsEngineSpecialized)

	import io.legs.utils.ActionTokenizer._

	def executeStep(step: Step, state: State, userSpecialized : List[Specialization] = Nil)(implicit willWait: Duration = waitFor) : RoutableFuture = {
		try {
			(getInputs(tokenized(step.action.toList)) match {
				case m :: Nil => throw new Throwable("a step has to have at least a module and the command")
				case m :: c :: xs if m.s.contains(".") => // check if the module name has already full path
					(m.s, c.s, xs)
				case c :: xs =>
					val moduleName = (registeredSpecializedClasses ::: userSpecialized ).find(sp =>
						sp.getRoute(c.s, xs.length).isDefined
					).getOrElse(throw new Throwable(s"could not resolve route for ${c.s} args:${step.action}"))
					  .getClass.getName.replace("$", "")
					(moduleName, c.s, xs)
			}) match {
				case (moduleName, commandName, args) =>
					args.reverse.foldLeft(List.empty[String], state) {
						case ((_args, _state), KeyToken(s)) => (s :: _args, _state)
						case ((_args, _state), ValueToken(s)) =>
							val uid = UUID.randomUUID().toString
							(uid :: _args, _state.updated(uid, Json.parse(s)))
					} match {
						case (_args, _state) =>
							Specialization.synchronized {
								import scala.reflect.runtime.universe
								val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
								val module = runtimeMirror.staticModule(moduleName)

								runtimeMirror.reflectModule(module)
							}.instance match {
								case specInstance: Specialization =>
									specInstance.invokeAction(commandName, _args, _state, step.values.getOrElse(Map()))
								case _ =>
									Future.failed(new Exception(s"The class $moduleName is not extending 'Specialization' "))
							}
					}
			}

		} catch {
			case e: Throwable => Future.failed(e)
		}
	}

	private def allParamsDefined(params: List[String], state: Specialization.State): Boolean =
		params.forall(state.contains)


	private def getMissingParameters(params: List[String], state: Specialization.State) : List[String] =
		params.filterNot(state.contains)


	private def resolveParamValues(paramNames: List[String], state: State) : List[Any] =
		paramNames.map(p=>state.get(p).get).toList

	private def placeholder : RoutableFuture = Future.failed(new Throwable("should not be run"))
	private val compareType = getClass.getDeclaredMethods.find( _.getName == "placeholder" ).get.getGenericReturnType


}
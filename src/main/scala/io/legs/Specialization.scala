package io.legs

import java.util.logging.{Level, Logger}

import io.legs.specialized._
import io.legs.utils.JsonFriend
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}


trait Specialization {

	import io.legs.Specialization._

	private lazy val spcializationLogger = Logger.getLogger(this.getClass.getSimpleName)

	private def routes =
		this.getClass.getMethods.toList.filter(_.getGenericReturnType == compareType).map(m=> (m.getName,m.getParameterTypes,m ) )

	private def getRoute(name:String, numArgs:Int) =
		// need to add 1 to count since the first parameter is state, which should always be there
		// need to subtract 1 to count since the implicit context
		routes.find( r=> r._1 == name && (r._2.length - (if (r._2.last == classOf[ExecutionContext]) 1 else 0 )) == numArgs +1 )

	def invokeAction(name: String, paramNames: List[String], state: Specialization.State, actionValues:Map[String,JsValue])(implicit ctx : ExecutionContext) : RoutableFuture = {
		spcializationLogger.info(s"attempting to invoke action name:$name")
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
					try {
						val params =
							if (route._2.last == classOf[ExecutionContext])
								state :: resolvedArgs ::: List(ctx)
							else
								state :: resolvedArgs
						route._3.invoke(this, params.toSeq.asInstanceOf[Seq[Object]] : _* ).asInstanceOf[RoutableFuture]
					} catch {
						case e:Exception =>
							spcializationLogger.log(Level.SEVERE,"failing invocation of:$name",e)
							Future(Failure(new Throwable(s"failing invocation of:$name",e)))
					}
				} else {
					val msg = s"could not locate some parameters in state:${Specialization.getMissingParameters(paramNames, paramResolver).mkString(",")}"
					spcializationLogger.log(Level.SEVERE,msg)
					Future(Failure(new Throwable(msg)))
				}

			case None =>
				spcializationLogger.log(Level.SEVERE,s"could not find method $name")
				Future(Failure(new Throwable(s"could not find method $name")))
		}

	}
}

object Specialization {

	case class Yield(valueOpt:Option[Any])

	type State = Map[String,Any]
	type RoutableFuture = Future[Try[Yield]]

	implicit val pool = ExecutionContext.fromExecutor(new scala.concurrent.forkjoin.ForkJoinPool)

	val tenSecDuration = Duration(10, "second")
	val oneMinuteDuration = Duration(60, "second")

	implicit val waitFor = Duration(60, "second")

	lazy val registeredSpecializedClasses = List(Casts,LinkTracker,Numbers,Persistor,Queue,SimpleScraper,Strings,Tools,WebDriver,MapReduce)

	def executeStep(step: Step, state: State)(implicit willWait: Duration = waitFor) : RoutableFuture = {

		try {

			val routeParts = step.action.split("/")

			val (specializedClass, specializedAction, paramNames) =
				if (step.action.contains(".")) {
					(routeParts(0), routeParts(1), routeParts.slice(2, routeParts.length).toList)
				} else {
					val actionName = routeParts(0)
					val args = routeParts.slice(1, routeParts.length).toList
					val routeName = registeredSpecializedClasses.find(sp =>
						sp.getRoute(actionName, args.length).isDefined
					).getOrElse(throw new Throwable(s"could not resolve route for $actionName args:${args.mkString(",")}"))
							.getClass.getName.replace("$", "")
					(routeName, actionName, args)
				}

			Specialization.synchronized {
				import scala.reflect.runtime.universe
				val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
				val module = runtimeMirror.staticModule(specializedClass)

				runtimeMirror.reflectModule(module)
			}.instance match {
				case specInstance: Specialization =>
					specInstance.invokeAction(specializedAction, paramNames, state, step.values.getOrElse(Map()))
				case _ =>
					Future(Failure(new Exception(s"The class $specializedClass is not extending 'Specialization' ")))
			}

		} catch {
			case e : Throwable => Future.failed(e)
		}

	}

	private def allParamsDefined(params: List[String], state: Specialization.State): Boolean =
		params.forall(state.contains)


	private def getMissingParameters(params: List[String], state: Specialization.State) : List[String] =
		params.filterNot(state.contains)


	private def resolveParamValues(paramNames: List[String], state: State) : List[Any] =
		paramNames.map(p=>state.get(p).get).toList

	private def placeholder : RoutableFuture = Future(Failure(new Throwable("should not be run")))
	private val compareType = getClass.getDeclaredMethods.find( _.getName == "placeholder" ).get.getGenericReturnType


}
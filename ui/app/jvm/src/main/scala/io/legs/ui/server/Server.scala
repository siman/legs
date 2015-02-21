package io.legs.ui.server

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import io.legs.ui.shared.nozzle.CRUDService
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, Json}
import spray.can.Http
import spray.can.Http.ConnectionClosed
import spray.http.HttpMethods._
import spray.http.Uri.Path.{Empty, Segment, Slash}
import spray.http._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Properties}
//
//object Router extends autowire.Server[String, upickle.Reader, upickle.Writer] with Implicits {
//	override def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
//	override def write[Result: upickle.Writer](r: Result) = upickle.write(r)
//
//	val routes = Router.route[Api](ApiService)
//
//	override implicit def Tuple2R[T1, T2](implicit evidence$1: R[T1], evidence$2: R[T2]): R[(T1, T2)] = ???
//
//	override implicit def Tuple2W[T1, T2](implicit evidence$3: W[T1], evidence$4: W[T2]): W[(T1, T2)] = ???
//}

class Server(models : List[CRUDService]) extends Actor {

	/**
	 * API -
	 *
	 * get[Model](atgs...)
	 * update[Model](model : Model, args : Map[String,String])
	 * create[Model](model : Model)
	 * delete[Model](id : String)
	 */

	implicit val ec : ExecutionContext = context.dispatcher

	final val logger = Logger(LoggerFactory.getLogger(getClass))

	override def receive: Receive = {


		case _ : Http.Connected =>
			sender ! Http.Register(self)


		case HttpRequest(method, uri ,_,entity,_) =>

			uri.path match {
				case Slash(Segment(controllerName,Empty)) =>
					triggerRoute(sender(),method,controllerName,None,uri.query.toMap,entityData(entity))
				case Slash(Segment(controllerName,Slash(Empty))) =>
					triggerRoute(sender(),method,controllerName,None,uri.query.toMap,entityData(entity))
				case Slash(Segment(controllerName, Slash(tail))) =>
					triggerRoute(sender(),method,controllerName,Some(tail.toString()),uri.query.toMap,entityData(entity))
				case _ =>
					logger.error(s"could not understand request:${uri.toString()}")
					sender ! HttpResponse(400)
			}

		case cc : ConnectionClosed => logger.debug(s"connection closed ${cc.toString}")
		case Timedout(req) => logger.info(s"timeoed out: ${req.toString} ")


		case fallback =>
			logger.error(s"did not understand message:$fallback")

			sender ! HttpResponse(status = StatusCode.int2StatusCode(404), entity =
				"""
				  |You found it!
				  |Its the unknown knowledge
				  |Not all is left for you is to invent it!
				  |Come right back to add it here!!
				""".stripMargin)
	}

	private def normalizeName(clazz : CRUDService) : String =
		normalizeName(clazz.getClass.getSimpleName)

	private def normalizeName(str : String) : String =
		str.replace("$","").toLowerCase

	private[this] lazy val getModelMapping : Map[String,CRUDService] =
		models.map {
			m=> normalizeName(m) -> m
		}.toMap

	private def triggerRoute(sender : ActorRef ,method: HttpMethod, action : String,
		actionTail : Option[String], query : Map[String,String] = Map(),
		body : Option[String] = None )
		(implicit ec : ExecutionContext): Unit =
	{
		val resF = getModelMapping.get(normalizeName(action)) match {
			case Some(model) =>
				val niceQ = query.toList.map(x=>x._1 + "=" + x._2).mkString("&")
				logger.info(s"~ $method ${action.toLowerCase}/${actionTail.getOrElse("")}?$niceQ (${body.getOrElse("")})")

				method match {
					case GET =>
						model.read(actionTail, query)
					case POST =>
						if (body.isDefined)
							model.fmt.reads(Json.parse(body.get)) match {
								case JsSuccess(v,_) => model.create(v,query)
								case JsError(errs) =>  throw new Throwable("error parsing" + errs.toString())
							}
						else
							Future.failed(new Throwable("create needs body"))
					case PUT =>
						if (body.isDefined)
							model.fmt.reads(Json.parse(body.get)) match {
								case JsSuccess(v,_) => model.update(v,query)
								case JsError(errs) =>  throw new Throwable("error parsing" + errs.toString())
							}
						else
							Future.failed(new Throwable("update needs body"))

					case _ => Future.failed(new Throwable("could not resolve method:" + method))
				}
			case None=>
				logger.error(s"route failed $method $action $actionTail $query")
				Future.failed(new Throwable("could not resolve route"))
		}

		resF.onComplete {
			case Success(result) =>
				sender ! HttpResponse(200, result.toString)

			case Failure(e)=>
				val q = query.toList.map(x=> x._1 + "=" + x._2).mkString("&")
				val path = action + (actionTail match {
					case Some(t)=> "/" + t
					case None => ""
				})
				logger.error(s"error in route $method /$path?$q",e)
				sender ! HttpResponse(400,e.getMessage)
		}

	}

	private[this] def entityData(entity : HttpEntity) : Option[String] =
		if (entity.nonEmpty) Some(entity.asString)
		else None

//	def oldreceive: Receive = runRoute {
//		get {
//			pathSingleSlash {
//				complete {
//					HttpEntity(
//						MediaTypes.`text/html`,
//						Page.skeleton
//					)
//				}
//			} ~
//				getFromResourceDirectory("app")
//		} ~
//			post {
//				path("ajax" / "list") {
//					extract(_.request.entity.asString) { e=>
//						complete {
//							upickle.write(list(e))
//						}
//					}
//				} ~
//					path("api" / Segments) { s =>
//						import upickle._
//						extract(_.request.entity.asString) { e =>
//							onComplete(
//								Router.routes(autowire.Core.Request(s, upickle.read[Map[String, String]](e)))
//							){
//								case Success(output : String) => complete(HttpResponse(200, HttpEntity(`application/json`,output)))
//								case Failure(e: Throwable) => complete(HttpResponse(500, entity = e.getMessage))
//							}
//
//						}
//					}
//			}
//
//	}

//	def list(path: String) = {
//		val (dir, last) = path.splitAt(path.lastIndexOf("/") + 1)
//		val files =
//			Option(new java.io.File("./" + dir).listFiles())
//				.toSeq.flatten
//		for{
//			f <- files
//			if f.getName.startsWith(last)
//		} yield (f.getName, f.length())
//	}

}

object Server {

	def main(args : Array[String]) : Unit = {
		implicit val system = ActorSystem()
		val port = Properties.envOrElse("PORT","8080").toInt
		val serviceActor = system.actorOf(Props(new Server(Nil)),"Server-Actor")
		
		IO(Http).ask(Http.Bind(serviceActor, "0.0.0.0", port, 100, Nil, None))(Timeout(5,TimeUnit.SECONDS)).flatMap {
			case b: Http.Bound ⇒ Future.successful(b)
			case Tcp.CommandFailed(b: Http.Bind) ⇒
				// TODO: replace by actual exception when Akka #3861 is fixed.
				//       see https://www.assembla.com/spaces/akka/tickets/3861
				Future.failed(new RuntimeException(
					"Binding failed. Switch on DEBUG-level logging for `akka.io.TcpListener` to log the cause."))
		}(system.dispatcher)

	}

}



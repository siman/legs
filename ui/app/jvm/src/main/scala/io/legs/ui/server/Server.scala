package io.legs.ui.server

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.{IO, Tcp}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import io.legs.ui.server.service.JobsService
import io.nozzler.CRUDService
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsError, JsSuccess, Json}
import spray.can.Http
import spray.http.ContentTypes._
import spray.http.HttpMethods._
import spray.http._
import spray.routing.HttpServiceActor
import spray.routing.directives.{ContentTypeResolver, FileAndResourceDirectives}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Properties, Success}


class Server(models : List[CRUDService]) extends HttpServiceActor {

	/**
	 * API -
	 *
	 * get[Model](atgs...)
	 * update[Model](model : Model, args : Map[String,String])
	 * create[Model](model : Model)
	 * delete[Model](id : String)
	 */

	implicit val ec : ExecutionContext = context.dispatcher

	implicit final val logger = Logger(LoggerFactory.getLogger(getClass))

	import ContentTypeResolver.Default
	val resourceRouter = new FileAndResourceDirectives(){}

//	override def receive: Receive = {
//
//
//		case _ : Http.Connected =>
//			sender ! Http.Register(self)
//
//
//		case HttpRequest(GET, Uri.Path("/"),_,_,_) => sender ! HttpResponse(200,HttpEntity(MediaTypes.`text/html`,Page.skeleton))
//		case r @ HttpRequest(GET, Uri(_,_,Slash(Segment("js",tail)),_,_),_,_,_) =>
//			resourceRouter.getFromResourceDirectory("app").apply(RequestContext(r,sender(),tail))
//
//
//		case HttpRequest(method, uri @ Uri(_,_,Slash(Segment("api", Slash(tail))),query,_) ,_,entity,_) =>
//			tail match {
//				case Segment(controllerName,Empty) =>
//					triggerRoute(sender(),method,controllerName,None,query.toMap,entityData(entity))
//				case Segment(controllerName,Slash(Empty)) =>
//					triggerRoute(sender(),method,controllerName,None,query.toMap,entityData(entity))
//				case Segment(controllerName, Slash(_tail)) =>
//					triggerRoute(sender(),method,controllerName,Some(_tail.toString()),query.toMap,entityData(entity))
//				case _ =>
//					logger.error(s"could not understand request:${tail.toString()}")
//					sender ! HttpResponse(400)
//			}
//
//		case cc : ConnectionClosed => logger.debug(s"connection closed ${cc.toString}")
//		case Timedout(req) => logger.info(s"timeoed out: ${req.toString} ")
//
//
//		case fallback =>
//			logger.error(s"did not understand message:$fallback")
//
//			sender ! HttpResponse(status = StatusCode.int2StatusCode(404), entity =
//				"""
//				  |You found it!
//				  |Its the unknown knowledge
//				  |Not all is left for you is to invent it!
//				  |Come right back to add it here!!
//				""".stripMargin)
//	}

	private def normalizeName(clazz : CRUDService) : String =
		normalizeName(clazz.getClass.getSimpleName)

	private def normalizeName(str : String) : String =
		str.replace("$","").toLowerCase

	private[this] lazy val getModelMapping : Map[String,CRUDService] =
		models.map {
			m=> normalizeName(m.meta.schema) -> m
		}.toMap

//	private def triggerRoute(sender : ActorRef ,method: HttpMethod, action : String,
//		actionTail : Option[String], query : Map[String,String] = Map(),
//		body : Option[String] = None )
//		(implicit ec : ExecutionContext): Unit =
//	{
//		val resF = getModelMapping.get(normalizeName(action)) match {
//			case Some(model) =>
//				val niceQ = query.toList.map(x=>x._1 + "=" + x._2).mkString("&")
//				logger.info(s"~ $method ${action.toLowerCase}/${actionTail.getOrElse("")}?$niceQ (${body.getOrElse("")})")
//
////				???
//				method match {
//					case GET =>
//						model.read(actionTail, query)
//					case POST =>
//						if (body.isDefined)
//							model.fmt.reads(Json.parse(body.get)) match {
//								case JsSuccess(v,_) => model.create(v,query)
//								case JsError(errs) =>  throw new Throwable("error parsing" + errs.toString())
//							}
//						else
//							Future.failed(new Throwable("create needs body"))
//					case PUT =>
//						if (body.isDefined)
//							model.fmt.reads(Json.parse(body.get)) match {
//								case JsSuccess(v,_) => model.update(v,query)
//								case JsError(errs) =>  throw new Throwable("error parsing" + errs.toString())
//							}
//						else
//							Future.failed(new Throwable("update needs body"))
//
//					case _ => Future.failed(new Throwable("could not resolve method:" + method))
//				}
//			case None=>
//				logger.error(s"route failed $method $action $actionTail $query")
//				Future.failed(new Throwable("could not resolve route"))
//		}
//
//		resF.onComplete {
//			case Success(result) =>
//				sender ! HttpResponse(200, result.toString)
//
//			case Failure(e)=>
//				val q = query.toList.map(x=> x._1 + "=" + x._2).mkString("&")
//				val path = action + (actionTail match {
//					case Some(t)=> "/" + t
//					case None => ""
//				})
//				logger.error(s"error in route $method /$path?$q",e)
//				sender ! HttpResponse(400,e.getMessage)
//		}
//
//	}

	private[this] def entityData(entity : HttpEntity) : Option[String] =
		if (entity.nonEmpty) Some(entity.asString)
		else None

	import spray.http.Uri.Path._
	def receive: Receive = runRoute {
		get {
			pathSingleSlash {
				complete {
					HttpEntity(
						MediaTypes.`text/html`,
						Page.skeleton
					)
				}
			} ~
				getFromResourceDirectory("app")
		} ~
			extract(_.request) {
				case HttpRequest(method, Uri(_, _, spray.http.Uri.Path.Slash(spray.http.Uri.Path.Segment("api", spray.http.Uri.Path.Slash(tail))), query, _), _, entity, _) =>
					onComplete(tail match {
						case spray.http.Uri.Path.Segment(controllerName, Empty) =>
							newTriggerRotoute(method, controllerName, None, query.toMap, entityData(entity))
						case spray.http.Uri.Path.Segment(controllerName, spray.http.Uri.Path.Slash(Empty)) =>
							newTriggerRotoute(method, controllerName, None, query.toMap, entityData(entity))
						case spray.http.Uri.Path.Segment(controllerName, spray.http.Uri.Path.Slash(_tail)) =>
							newTriggerRotoute(method, controllerName, Some(_tail.toString()), query.toMap, entityData(entity))
						case _ =>
							Future.failed(new Throwable(s"could not understand request:${tail.toString()}"))
					}) {
						case Success(None) => complete(HttpResponse(200))
						case Success(Some(v: String)) => complete(HttpResponse(200, HttpEntity(`application/json`, v)))
						case Success(unknown) =>
							logger.info("could not understand success message" + unknown.toString)
							complete(HttpResponse(500,"unknown result"))
						case Failure(e: Throwable) =>
							complete(HttpResponse(500, HttpEntity(`application/json`,
								s"""
								   |{"error":"${e.getMessage}"
								""".stripMargin)))
					}

			}
	}


	private def newTriggerRotoute(method: HttpMethod, action : String, actionTail : Option[String], query : Map[String,String] = Map(), body : Option[String] = None )
			(implicit ec : ExecutionContext): Future[Option[String]] =

		(getModelMapping.get(normalizeName(action)) match {
			case Some(model) =>
				val niceQ = query.toList.map(x=>x._1 + "=" + x._2).mkString("&")

				logger.info(s"~ $method ${action.toLowerCase}/${actionTail.getOrElse("")}?$niceQ (${body.getOrElse("")})")

				method match {
					case GET =>
						model.read(actionTail, query) map {
							case Nil => Option.empty[String]
							case readModels => Some(JsArray(readModels.map(model.fmt.writes)).toString())
						}
					case POST =>
						if (body.isDefined)
							model.fmt.reads(Json.parse(body.get)) match {
								case JsSuccess(v,_) => model.create(v,query).map(Some(_))
								case JsError(errs) =>  Future.failed(new Throwable("error parsing" + errs.toString()))
							}
						else Future.failed(new Throwable("create needs body"))
					case PUT =>
						if (body.isDefined)
							model.fmt.reads(Json.parse(body.get)) match {
								case JsSuccess(v,_) => model.update(v,query).map(_ => Option.empty[String])
								case JsError(errs) =>  Future.failed(new Throwable("error parsing" + errs.toString()))
							}
						else Future.failed(new Throwable("update needs body"))

					case _ => Future.failed(new Throwable("could not resolve method:" + method))
				}
			case None=> Future.failed(new Throwable(s"could not resolve route ${prettyRoute(method.name,action,actionTail,query)}"))
		}).andThen {
			case Failure(e : Throwable) =>
				logger.error(s"error in route  ${prettyRoute(method.name,action,actionTail,query)}",e)
		}


	private def prettyRoute(method : String, action : String, actionTail : Option[String], query : Map[String,String]) : String = {
		val q = query.toList match {
			case Nil => ""
			case xs => "?" + xs.map(x=> x._1 + "=" + x._2).mkString("&")
		}
		val path = action + (actionTail match {
			case Some(t)=> "/" + t
			case None => ""
		})
		s"$method $path$q"
	}



	logger.info(s"bound server to endpoints:(${getModelMapping.keys.mkString(",")})")



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
		val serverActor = system.actorOf(Props(new Server(JobsService :: Nil)),"Server-Actor")

		IO(Http).ask(Http.Bind(serverActor, "0.0.0.0", port, 100, Nil, None))(Timeout(5,TimeUnit.SECONDS)).flatMap {
			case b: Http.Bound ⇒ Future.successful(b)
			case Tcp.CommandFailed(b: Http.Bind) ⇒
				// TODO: replace by actual exception when Akka #3861 is fixed.
				//       see https://www.assembla.com/spaces/akka/tickets/3861
				Future.failed(new RuntimeException(
					"Binding failed. Switch on DEBUG-level logging for `akka.io.TcpListener` to log the cause."))
		}(system.dispatcher)



	}

}



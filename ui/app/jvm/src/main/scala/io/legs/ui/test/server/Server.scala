package io.legs.ui.test.server

import akka.actor.ActorSystem
import io.legs.ui.shared.Api
import spray.http.{HttpEntity, MediaTypes}
import spray.routing.SimpleRoutingApp
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Properties

object Router extends autowire.Server[String, upickle.Reader, upickle.Writer] {
	def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
	def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

object Server extends SimpleRoutingApp {

	val apiService = new ApiService

	def main(args : Array[String]) : Unit = {
		implicit val system = ActorSystem()
		val port = Properties.envOrElse("PORT","8080").toInt
		startServer("0.0.0.0",port = port){
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
			post {
				path("ajax" / "list") {
					extract(_.request.entity.asString) { e=>
						complete {
							upickle.write(list(e))
						}
					}
				} ~
					path("api" / Segments) { s =>
						extract(_.request.entity.asString) { e =>
							complete {
								// handle API requests via autowire
								Router.route[Api](apiService)(
									autowire.Core.Request(s, upickle.read[Map[String, String]](e))
								)
							}
						}
					}
			}
		}

		def list(path: String) = {
			val (dir, last) = path.splitAt(path.lastIndexOf("/") + 1)
			val files =
				Option(new java.io.File("./" + dir).listFiles())
					.toSeq.flatten
			for{
				f <- files
				if f.getName.startsWith(last)
			} yield (f.getName, f.length())
		}

	}

}

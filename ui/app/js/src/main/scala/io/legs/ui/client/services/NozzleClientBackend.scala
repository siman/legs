package io.legs.ui.client.services

import io.nozzler.NozzleBackend
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.URIUtils._
import scala.util.Failure


object NozzleClientBackend extends NozzleBackend {

	val apiPrefix = "/api"

	override def processMessage(resource: String, method: String, model: Option[String], uid: Option[String], query: Map[String, String]): Future[Option[String]] =
		dom.ext.Ajax(
			method = methodMapping(method),
			url = urlGenerator(resource,uid,query),
			data = model.getOrElse(""),
			timeout = 10000,
			headers = Map(),
			withCredentials = false,
			responseType = ""
		)
		.map(_.responseText)
		.map {
			case "" => Option.empty[String]
			case body => Some(body)
		}.andThen {
			case Failure(e) => println("error processing message for url:" + urlGenerator(resource,uid,query),e)
			case ignore =>
		}


	def urlGenerator(resource : String, uid : Option[String], query : Map[String,String]) : String =
		s"$apiPrefix/$resource${ uid.map("/" + _).getOrElse("") }" +
			(query.size match {
				case 0 => ""
				case _ => "?" + query.toList.map {
					case (k,v) => encodeURI(k) + "=" + encodeURI(v)
				}.mkString("&")
			})


	private def methodMapping(crudMethod : String) : String = crudMethod match {
		case "create" => "POST"
		case "read" => "GET"
		case "update" => "PUT"
		case "delete" => "DELETE"
	}


}

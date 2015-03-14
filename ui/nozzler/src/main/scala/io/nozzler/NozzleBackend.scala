package io.nozzler

import scala.concurrent.Future


trait NozzleBackend {
	def processMessage(resource: String, method : String, model : Option[String],uid : Option[String], query : Map[String,String]) : Future[Option[String]]
}
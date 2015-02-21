package io.legs.ui.shared.nozzle

import scala.concurrent.Future

trait NozzleBackend {
	def processMessage(resource: String, method : String, model : Option[String] = None,
		uid : Option[String] = None, query : Map[String,String] = Map.empty[String,String]) : Future[Option[String]]
}

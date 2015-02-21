package io.legs.ui.shared.nozzle

import play.api.libs.json.Format

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros

trait CRUDService {

	type ModelType

	implicit val fmt : Format[ModelType]

	private lazy val cachedSchemeName = getClass.getSimpleName.toLowerCase.replace("$","")

	def schemeName : String = cachedSchemeName

	def create(m : ModelType, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[String]
	def read(uid : Option[String] = None, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[List[ModelType]]
	def update(m : ModelType, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[Unit]
	def delete(uid : String, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[Boolean]

	//TODO: look into
	def read(uid : String, query: Map[String,String])(implicit  executor: ExecutionContext) : Future[ModelType] = ???
	def read(query: Map[String,String])(implicit  executor: ExecutionContext) : Future[List[ModelType]] = ???

}

object CRUDService {
	

}







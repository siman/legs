package io.nozzler

import play.api.libs.json.Format

import scala.concurrent.{ExecutionContext, Future}

trait CRUDService {

	type ModelType

	val meta : CRUDMeta

	val fmt : Format[ModelType]

	def create(m : ModelType, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[String]
	def read(uid : Option[String] = None, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[List[ModelType]]
	def update(m : ModelType, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[Unit]
	def delete(uid : String, query: Map[String,String] = Map())(implicit  executor: ExecutionContext) : Future[Boolean]


	def readToJson(uid : Option[String] = None, query: Map[String,String] = Map())(implicit  executor: ExecutionContext, lstFmt : Format[List[ModelType]]) : Future[Option[String]] =
		read(uid,query).map {
			case Nil => Option.empty[String]
			case models => Some(lstFmt.writes(models).toString())
		}

	//TODO: look into
	//	def read(uid : String, query: Map[String,String])(implicit  executor: ExecutionContext) : Future[ModelType] = ???
	//	def read(query: Map[String,String])(implicit  executor: ExecutionContext) : Future[List[ModelType]] = ???

}
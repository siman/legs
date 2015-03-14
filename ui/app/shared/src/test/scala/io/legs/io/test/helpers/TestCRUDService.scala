package io.legs.io.test.helpers

import io.nozzler.{CRUDMeta, CRUDService}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

case class TestCRUDService(name : String)

object TestCRUDService extends CRUDService {

	type ModelType = TestCRUDService

	implicit val fmt = Json.format[ModelType]

	val meta = CRUDMeta[ModelType]

	override def create(m: ModelType, query: Map[String, String])(implicit executor: ExecutionContext): Future[String] = ???

	override def update(m: ModelType, query: Map[String, String])(implicit executor: ExecutionContext): Future[Unit] = ???

	override def delete(uid: String, query: Map[String, String])(implicit executor: ExecutionContext): Future[Boolean] = ???

	override def read(uid: Option[String], query: Map[String, String])(implicit executor: ExecutionContext): Future[List[ModelType]] = Future.successful(TestCRUDService("good!") :: Nil)

}




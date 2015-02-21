package io.legs.ui.test.helpers

import io.legs.ui.shared.nozzle.CRUDService
import play.api.libs.json.{Format, Json}

import scala.concurrent.{ExecutionContext, Future}

case class Test(name : String)

object Test extends CRUDService {

	type ModelType = Test

	override def create(m: Test, query: Map[String, String])(implicit executor: ExecutionContext): Future[String] = ???

	override def update(m: Test, query: Map[String, String])(implicit executor: ExecutionContext): Future[Unit] = ???

	override def delete(uid: String, query: Map[String, String])(implicit executor: ExecutionContext): Future[Boolean] = ???

	override def read(uid: Option[String], query: Map[String, String])(implicit executor: ExecutionContext): Future[List[Test]] = Future.successful(Test("good!") :: Nil)

	override implicit val fmt: Format[Test] = Json.format[Test]
}




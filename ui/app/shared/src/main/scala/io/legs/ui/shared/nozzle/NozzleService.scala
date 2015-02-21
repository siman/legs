package io.legs.ui.shared.nozzle

import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object NozzleService {

	def serviceMacroImpl[T : c.WeakTypeTag](c : blackbox.Context)(backend: c.Expr[NozzleBackend]) = {
		import c.universe._

		c.Expr[NozzleService[T]](q"""
	  	new NozzleService[${c.weakTypeOf[T]}]($backend,${c.weakTypeOf[T].toString.split('.').last.toLowerCase})
		""")

	}

	def apply[T](implicit backend: NozzleBackend) : NozzleService[T] = macro serviceMacroImpl[T]

}

class NozzleService[T](backend : NozzleBackend, resource : String)(implicit val fmt: Format[T]) extends CRUDService {

	override type ModelType = T

	//	import scala.language.experimental.macros
	//	def read[T <: CRUDService#ModelType](uid : Option[String] = None, query: Map[String,String] = Map()) : Future[T] = macro NozzleService.serviceMacro[T]

	override def create(m: T, query: Map[String, String])(implicit executor: ExecutionContext): Future[String] =
		backend.processMessage(resource,"create", Some(fmt.writes(m).toString()), query = query)
			.map(_.getOrElse(throw new Throwable("could not find new ID in response")))

	override def read(uid: Option[String], query: Map[String, String])(implicit executor: ExecutionContext): Future[List[T]] =
		backend.processMessage(resource, "read", uid = uid, query = query)
			.map {
				case None => throw new Throwable("unexpected null response")
				case Some(response) =>
					Json.fromJson[List[T]](Json.parse(response)) match {
						case JsSuccess(result, _) => result
						case JsError(err) => throw new Throwable("error parsing response" + err)
					}
		}

	override def update(m: T, query: Map[String, String])(implicit executor: ExecutionContext): Future[Unit] =
		backend.processMessage(resource,"update", Some(fmt.writes(m).toString()), query = query)
			.map( _ => Unit )

	override def delete(uid: String, query: Map[String, String])(implicit executor: ExecutionContext): Future[Boolean] =
		backend.processMessage(resource,"delete", uid = Some(uid),query = query)
			.map( _.isDefined )

}
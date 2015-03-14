package io.nozzler


import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object NozzleService {

	import scala.language.experimental.macros
	
	def create[T](model: T, query: Map[String, String])(implicit executor: ExecutionContext, backend : NozzleBackend, pickler : upickle.Writer[T]): Future[String] =
		macro createImpl[T]

	def read[T](uid: Option[String], query: Map[String, String])(implicit executor: ExecutionContext, backend : NozzleBackend, unpickler : upickle.Reader[List[T]]): Future[List[T]] =
		macro readImpl[T]

	def createImpl[T : c.WeakTypeTag](c : blackbox.Context)(model: c.Expr[T], query: c.Expr[Map[String, String]])
			(executor: c.Expr[ExecutionContext], backend : c.Expr[NozzleBackend], pickler : c.Expr[upickle.Writer[T]]): c.Expr[Future[String]] = {
		import c.universe._
		val serviceName = c.weakTypeOf[T].toString.split('.').last.toLowerCase
		val tpe = c.weakTypeOf[T]

		c.Expr[Future[String]](q"""
		import scala.concurrent.ExecutionContext
  		implicit val pickler : upickle.Writer[$tpe] = $pickler
		$backend.processMessage($serviceName, "create", Some(upickle.write[$tpe]($model)), None, $query)
		""")
	}

	def readImpl[T : c.WeakTypeTag](c : blackbox.Context)(uid: c.Expr[Option[String]], query: c.Expr[Map[String, String]])
			(executor: c.Expr[ExecutionContext], backend : c.Expr[NozzleBackend], unpickler: c.Expr[upickle.Reader[List[T]]]): c.Expr[Future[List[T]]] = {
		import c.universe._
		val serviceName = c.weakTypeOf[T].toString.split('.').last.toLowerCase
		val tpe = c.weakTypeOf[T]

		c.Expr[Future[List[T]]](q"""
		import scala.concurrent.ExecutionContext
		import scala.util.{Failure, Success}
  		implicit val unpickler : upickle.Reader[List[$tpe]] = $unpickler
		$backend.processMessage($serviceName, "read", None, None, $query)
			.map {
				case None => throw new Throwable("unexpected null response")
				case Some(response) =>
					try {
						upickle.read[List[$tpe]](response)
					} catch {
	  					case e : Throwable=> throw new Throwable("error parsing response:\n" + e.getMessage + "\n" + response)
					}
			}.andThen {
				case Failure(e) => println("error processing message GET",$serviceName,$query,e.getMessage)
				case ignore =>
			}
		""")

	}

//	override def update(m: T, query: Map[String, String])(implicit executor: ExecutionContext): Future[Unit] =
//		backend.processMessage(resource,"update", Some(upickle.write(m).toString), None, query)
//			.map( _ => Unit )
//
//	override def delete(uid: String, query: Map[String, String])(implicit executor: ExecutionContext): Future[Boolean] =
//		backend.processMessage(resource,"delete", None, Some(uid),query)
//			.map( _.isDefined )


}
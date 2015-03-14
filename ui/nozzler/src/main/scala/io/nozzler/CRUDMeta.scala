package io.nozzler

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

case class CRUDMeta(
	schema : String
)

object CRUDMeta {

	def apply[T] : CRUDMeta = macro createCRUDMeta[T]

	def createCRUDMeta[T : c.WeakTypeTag](c : blackbox.Context) : c.Expr[CRUDMeta] = {
		import c.universe._
		val typeName = weakTypeTag[T].tpe.typeSymbol.name.toString
		c.Expr[CRUDMeta](q"CRUDMeta($typeName)")
	}

}


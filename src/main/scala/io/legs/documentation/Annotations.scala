package io.legs.documentation

import scala.annotation.StaticAnnotation

object Annotations {

	case class LegsParamAnnotation(details : String) extends StaticAnnotation
	case class LegsFunctionAnnotation(details : String, yieldType : Any, yieldDetails : String) extends StaticAnnotation

}

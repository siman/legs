package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsParamAnnotation, LegsFunctionAnnotation}

import scala.concurrent.Future

object Numbers extends Specialization {

	@LegsFunctionAnnotation(
		details = "generated a list of numbers",
		yieldType = List.empty[Int],
		yieldDetails = "list of numbers"
	)
	def GENERATE(state: Specialization.State,
		start: BigDecimal @LegsParamAnnotation("start value"),
		end: BigDecimal @LegsParamAnnotation("end value")
	) : RoutableFuture =
			Future.successful(Yield(Some(Range(start.toInt, end.toInt).toList)))

}

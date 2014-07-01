package io.legs.specialized

import io.legs.Specialization
import scala.util.Success
import io.legs.Specialization.{RoutableFuture, Yield}
import scala.concurrent.Future

object Numbers extends Specialization {

	def GENERATE(state: Specialization.State, start: BigDecimal, end: BigDecimal) : RoutableFuture =
			Future.successful(Success(Yield(Some(Range(start.toInt, end.toInt).toList))))

}

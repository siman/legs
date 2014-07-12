package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization._

import scala.concurrent._

object Casts extends Specialization {

	def CAST(state: Specialization.State, input: Any, toType:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		input match {
			case _ : Int => toType match {
				case "String" => Future.successful(Yield(Some(input.toString)))
			}
			case default => Future.successful(Yield(Some(input)))
		}

}

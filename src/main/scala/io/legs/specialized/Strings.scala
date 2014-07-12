package io.legs.specialized

import io.legs.Specialization
import scala.util.{Failure, Success}
import io.legs.Specialization.{Yield, RoutableFuture}
import scala.concurrent._

object Strings extends Specialization {

	def EXTRACT_REGEX(state: Specialization.State, input:String, regex:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val r = regex.r
			input match {
				case r(output) => 	Success(Yield(Some(output)))
				case _ => 			Success(Yield(None))
			}
		}

	def REPLACE_REGEX(state:Specialization.State, input:String, matchStr:String, replaceStr:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val result = input.replaceAll(matchStr, replaceStr)
			Success(Yield(Some(result)))
		}

	def SPLIT(state:Specialization.State, input:String, splitBy:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			if (splitBy.length == 1){
				val result = input.split(splitBy.toCharArray.head).toList
				Success(Yield(Some(result)))
			} else {
				Failure(new Throwable("Strings/SPLIT currenly supports splitting by string of length 1"))
			}
		}

	def TRIM(state:Specialization.State, input:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Success(Yield(Some(input.trim())))
		}

}

package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}

import scala.concurrent._

object Strings extends Specialization {

	def EXTRACT_REGEX(state: Specialization.State, input:String, regex:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val r = regex.r
			input match {
				case r(output) => 	Yield(Some(output))
				case _ => 			Yield(None)
			}
		}

	def REPLACE_REGEX(state:Specialization.State, input:String, matchStr:String, replaceStr:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val result = input.replaceAll(matchStr, replaceStr)
			Yield(Some(result))
		}

	def SPLIT(state:Specialization.State, input:String, splitBy:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			if (splitBy.length == 1){
				val result = input.split(splitBy.toCharArray.head).toList
				Yield(Some(result))
			} else {
				throw new Throwable("Strings/SPLIT currenly supports splitting by string of length 1")
			}
		}

	def TRIM(state:Specialization.State, input:String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Yield(Some(input.trim()))
		}

}

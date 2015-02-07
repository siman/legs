package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsParamAnnotation, LegsFunctionAnnotation}

import scala.concurrent._

object StringsSpecialized extends Specialization {


	@LegsFunctionAnnotation(
		details = "extract value from input string using Regex",
		yieldType = "String",
		yieldDetails = "extracted value"
	)
	def EXTRACT_REGEX(state: Specialization.State, 
		input:String @LegsParamAnnotation("input string"),
		regex:String @LegsParamAnnotation("REGEX pattern to match against for extraction")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val r = regex.r
			input match {
				case r(output) => 	Yield(Some(output))
				case _ => 			Yield(None)
			}
		}

	@LegsFunctionAnnotation(
		details = "evaluate a regex expression over an input string, matching groups would be $1,$2.. etc in the replace ",
		yieldType = "String",
		yieldDetails = "extracted value"
	)
	def REPLACE_REGEX(state:Specialization.State,
		input:String @LegsParamAnnotation("input string"),
		matchRegex:String @LegsParamAnnotation("REGEX pattern for matching in groups $1, $2 etc.."),
		replaceExp:String @LegsParamAnnotation("REGEX replace pattern, use $1,$2 as place holders")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val result = input.replaceAll(matchRegex, replaceExp)
			Yield(Some(result))
		}

	@LegsFunctionAnnotation(
		details = "split input value into list of strings",
		yieldType = List.empty[String],
		yieldDetails = "list of resulting values after the split"
	)
	def SPLIT(state:Specialization.State,
		input:String @LegsParamAnnotation("input string"),
		splitBy:String @LegsParamAnnotation("character to split by")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			if (splitBy.length == 1){
				val result = input.split(splitBy.toCharArray.head).toList
				Yield(Some(result))
			} else {
				throw new Throwable("Strings/SPLIT currenly supports splitting by string of length 1")
			}
		}

	@LegsFunctionAnnotation(
		details = "trim some input string",
		yieldType = "String",
		yieldDetails = "string without white space characters around it"
	)
	def TRIM(state:Specialization.State,
		input:String @LegsParamAnnotation("input string")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Yield(Some(input.trim()))
		}

}

package io.legs.specialized

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.jsonpath.JsonPath
import io.legs.Specialization
import io.legs.Specialization._
import io.legs.documentation.Annotations.{LegsParamAnnotation, LegsFunctionAnnotation}

import scala.concurrent.{Future, ExecutionContext}


object JsonSpecialized extends Specialization {

	val mapper = new ObjectMapper
	def parseJson(s: String) = mapper.readValue(s, classOf[Object])

	@LegsFunctionAnnotation(
		details = "basically implementing https://github.com/gatling/jsonpath. Examples: https://github.com/gatling/jsonpath/blob/master/src/test/scala/io/gatling/jsonpath/JsonPathSpec.scala",
		yieldType = List.empty[String],
		yieldDetails = "returns list of matching evaluated XPATH expression"
	)
	def JSONPATH(state: Specialization.State,
		json : String @LegsParamAnnotation("json string value"),
		jsonPath: String @LegsParamAnnotation("json path expression")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			JsonPath.query(jsonPath,parseJson(json)) match {
				case Left(err)=> throw new Throwable(err.reason)
				case Right(it) => Yield(Some(it.toList))
			}

		}


}

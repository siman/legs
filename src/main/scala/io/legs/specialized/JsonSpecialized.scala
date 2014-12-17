package io.legs.specialized

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.jsonpath.JsonPath
import io.legs.Specialization
import io.legs.Specialization._

import scala.concurrent.{Future, ExecutionContext}


object JsonSpecialized extends Specialization {

	val mapper = new ObjectMapper
	def parseJson(s: String) = mapper.readValue(s, classOf[Object])

	/**
	 * basically implementing https://github.com/gatling/jsonpath
	 * Examples:
	 * https://github.com/gatling/jsonpath/blob/master/src/test/scala/io/gatling/jsonpath/JsonPathSpec.scala
	 * @param json String
	 * @param jsonPath String
	 * @return List[
	 */
	def JSONPATH(state: Specialization.State, json : String, jsonPath: String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			JsonPath.query(jsonPath,parseJson(json)) match {
				case Left(err)=> throw new Throwable(err.reason)
				case Right(it) => Yield(Some(it.toList))
			}

		}


}

package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import io.legs.utils.RedisProvider

import scala.concurrent._

object LinkTracker extends Specialization {

	final val linkTrackerPrefix_S = "legs:link_tracker:domain:"
	final def linkTrackerKey_S(domain: String) = s"$linkTrackerPrefix_S$domain"

	def checkExistCreate(domain:String, uri:String) : Boolean =
		RedisProvider.blocking {
			_.sadd(linkTrackerKey_S(domain),uri)
		} match {
			case 1 => false
			case _ => true
		}

	@LegsFunctionAnnotation(
		details = "check for existing UUID or create one if not previously existed",
		yieldType = Boolean,
		yieldDetails = "returns true if already exists, false if just created"
	)
	def CHECK_EXIST_CREATE(
		state: Specialization.State,
		domain : String @LegsParamAnnotation("the prefix (or domain) to be used for this resource") ,
		uri: String @LegsParamAnnotation("the resource URI (UID)")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Yield(Some(checkExistCreate(domain, uri)))
		}

}

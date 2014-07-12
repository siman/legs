package io.legs.specialized

import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
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


	def CHECK_EXIST_CREATE(state: Specialization.State, domain : String, uri: String)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			Yield(Some(checkExistCreate(domain, uri)))
		}

}

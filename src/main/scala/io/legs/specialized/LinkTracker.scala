package io.legs.specialized

import io.legs.Specialization
import io.legs.utils.RedisProvider
import scala.util.Success
import scala.concurrent._
import io.legs.Specialization.{RoutableFuture, Yield}

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
		future {
			Success(Yield(Some(checkExistCreate(domain, uri))))
		}

}

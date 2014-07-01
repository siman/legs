package helpers

import io.legs.Specialization
import scala.util.Success
import io.legs.Specialization.{RoutableFuture, Yield}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object TestSpecializer extends Specialization {

	private val counter = new scala.collection.mutable.HashMap[String, Int]()

	val paramPool = Map(
		"S1" -> "something",
		"S2" -> "something2",
		"S3" -> "something3",
		"N1" -> 10,
		"N2" -> 20,
		"N3" -> 30
	)

	def getKeyValue(key: String): Int =
		counter.getOrElse(key, 0)

	def get(state: Specialization.State,key:String) : RoutableFuture =
		Future.successful(Success(Yield(Some(getKeyValue(key)))))

	def incr(state: Specialization.State, key: String) : RoutableFuture =
		synchronized {
			counter.update(key, counter.getOrElseUpdate(key, 0) + 1)
			Future(Success(Yield(Some(counter.get(key)))))
		}


	def echo(state:Specialization.State,value:Any) : RoutableFuture =
			Future(Success(Yield(Some(value))))



	def incrState(state: Specialization.State, value: Int) : RoutableFuture =
		Future(Success(Yield(Some(value + 1))))

}

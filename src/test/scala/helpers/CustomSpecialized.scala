package helpers

import io.legs.Specialization
import io.legs.Specialization._

import scala.concurrent.Future

object CustomSpecialized extends Specialization {
	def CUSTOM_THING(state: Specialization.State,customValue : String) : RoutableFuture =
		Future {
			Yield(Some(customValue + "!"))
		}
}
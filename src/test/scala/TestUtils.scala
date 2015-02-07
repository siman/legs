import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TestUtils {

	val duration = Duration(2, "seconds")

	def toBlocking[T](future : Future[T]) : T = Await.result(future, duration)
}

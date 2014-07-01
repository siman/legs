import io.legs.Specialization
import io.legs.Specialization.Yield
import io.legs.specialized.Strings
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.FunSpec
import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class StringsSpecializedSpec extends FunSpec with MockFactory with AsyncAssertions {


	it("extracts from string with regex") {

		val res = Await.result(Strings.EXTRACT_REGEX(Map(),"aaaaa-bbbb","""^.*-(.*)$"""),
			Specialization.oneMinuteDuration)

		res match {
			case Success(Yield(outOpt)) =>
				assertResult("bbbb") { outOpt.get}
			case Failure(e)=>
				fail("Failure",e)
		}

	}

	it("replaces all in string with regex"){
		val res = Await.result(Strings.REPLACE_REGEX(Map(),"aaaaa-bbbb","""^.*-(.*)$""","""zzz$1"""),
			Specialization.oneMinuteDuration)

		res match {
			case Success(Yield(outOpt)) =>
				assertResult("zzzbbbb") { outOpt.get}
			case Failure(e)=>
				fail("Failure",e)
		}
	}


}

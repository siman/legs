import io.legs.Specialization
import io.legs.Specialization.Yield
import io.legs.specialized.StringsSpecialized
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSpec
import org.scalatest.concurrent.AsyncAssertions

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class StringsSpecializedSpec extends FunSpec with MockFactory with AsyncAssertions {


	it("extracts from string with regex") {

		val res = Await.result(StringsSpecialized.EXTRACT_REGEX(Map(),"aaaaa-bbbb","""^.*-(.*)$"""),
			Specialization.oneMinuteDuration)
		assertResult( Yield(Some("bbbb")) ) { res }

	}

	it("replaces all in string with regex"){
		val res = Await.result(StringsSpecialized.REPLACE_REGEX(Map(),"aaaaa-bbbb","""^.*-(.*)$""","""zzz$1"""),
			Specialization.oneMinuteDuration)

		assertResult( Yield(Some("zzzbbbb")) ) { res }

	}


}

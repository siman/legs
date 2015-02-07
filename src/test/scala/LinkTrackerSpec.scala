import io.legs.specialized.LinkTracker
import io.legs.utils.RedisProvider
import org.scalatest.{BeforeAndAfter, FunSpec}

class LinkTrackerSpec extends FunSpec with BeforeAndAfter {

	import TestUtils._

	val testDomain = "testing"
	val test1Uri = "test1"

	before {
		RedisProvider.drop("!!!")
	}

	it("shows an entry did not exist"){
		assertResult(false) { toBlocking(LinkTracker.checkExistCreate(testDomain, test1Uri)) }
	}

	it("shows an entry as existant when checking again"){
		assertResult(false) { toBlocking(LinkTracker.checkExistCreate(testDomain, test1Uri)) }
		assertResult(true) { toBlocking(LinkTracker.checkExistCreate(testDomain, test1Uri)) }
	}

	it("does not mix domains"){
		assertResult(false) { toBlocking(LinkTracker.checkExistCreate(testDomain, test1Uri)) }
		assertResult(true) {  toBlocking(LinkTracker.checkExistCreate(testDomain, test1Uri)) }
		assertResult(false) { toBlocking(LinkTracker.checkExistCreate(testDomain + "x", test1Uri)) }
	}

}

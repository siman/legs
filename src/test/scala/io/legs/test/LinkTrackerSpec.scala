package io.legs.test

import io.legs.specialized.LinkTrackerSpecialized
import io.legs.utils.RedisProvider
import org.scalatest.{BeforeAndAfter, FunSpec}

class LinkTrackerSpec extends FunSpec with BeforeAndAfter {

	import io.legs.test.TestUtils._

	val testDomain = "testing"
	val test1Uri = "test1"

	before {
		RedisProvider.drop("!!!")
	}

	it("shows an entry did not exist"){
		assertResult(false) { toBlocking(LinkTrackerSpecialized.checkExistCreate(testDomain, test1Uri)) }
	}

	it("shows an entry as existant when checking again"){
		assertResult(false) { toBlocking(LinkTrackerSpecialized.checkExistCreate(testDomain, test1Uri)) }
		assertResult(true) { toBlocking(LinkTrackerSpecialized.checkExistCreate(testDomain, test1Uri)) }
	}

	it("does not mix domains"){
		assertResult(false) { toBlocking(LinkTrackerSpecialized.checkExistCreate(testDomain, test1Uri)) }
		assertResult(true) {  toBlocking(LinkTrackerSpecialized.checkExistCreate(testDomain, test1Uri)) }
		assertResult(false) { toBlocking(LinkTrackerSpecialized.checkExistCreate(testDomain + "x", test1Uri)) }
	}

}

package io.legs.test

import io.legs.utils.InstructionsFileResolver
import org.scalatest.FunSpec

class InstructionsFilesSpec extends FunSpec {

	it("allows registration of additional resource folders"){
		val foundOpt = InstructionsFileResolver.getFile("test",List("src/test/scala/helpers/"))
		assertResult(true) { foundOpt.isDefined}
	}

}

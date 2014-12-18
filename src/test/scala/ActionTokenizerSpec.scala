import org.scalatest.FunSpec

class ActionTokenizerSpec extends FunSpec {

	import io.legs.utils.ActionTokenizer._

	it("tokenizes input correctly"){
		assertResult(StartSlash(Empty)) { tokenizd("/".toList) }
		assertResult(Empty) { tokenizd("".toList) }
		assertResult(StartSlash(KeyToken("body"))) { tokenizd("/body".toList) }
		assertResult(Segment(KeyToken("body"),Empty)) { tokenizd("body/".toList) }
		assertResult(Segment(KeyToken("body"),KeyToken("bodyz"))) { tokenizd("body/bodyz".toList) }
		assertResult(Segment(KeyToken("body"),Segment(KeyToken("bodyz"),KeyToken("bodykz")))) { tokenizd("body/bodyz/bodykz".toList) }
		assertResult(StartSlash(Segment(KeyToken("body"),Empty))) { tokenizd("/body/".toList) }
		assertResult(StartSlash(Segment(KeyToken("body"),KeyToken("text")))) { tokenizd("/body/text".toList) }
		assertResult(StartSlash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(KeyToken("txt2"),Empty))))) { tokenizd("/txt/txt1/txt2/".toList) }
		assertResult(StartSlash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(ValueToken("txt/2"),Empty))))) { tokenizd("/txt/txt1/${txt/2}/".toList) }
		assertResult(StartSlash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(KeyToken("${txt2\\}"),Empty))))) { tokenizd("/txt/txt1/${txt2\\}/".toList) }
	}


	it("gets a list of input tokens"){
		assertResult(List(KeyToken("txt"),KeyToken("txt1"),ValueToken("txt/2"))) { getInputs(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(ValueToken("txt/2"),Empty)))) }
	}


}

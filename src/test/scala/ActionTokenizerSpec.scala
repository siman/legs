import org.scalatest.FunSpec

class ActionTokenizerSpec extends FunSpec {

	import io.legs.utils.ActionTokenizer._

	it("tokenizes input correctly"){
		assertResult(Slash(Empty)) { tokenizeQuery("/".toList) }
		assertResult(Empty) { tokenizeQuery("".toList) }
		assertResult(Slash(KeyToken("body"))) { tokenizeQuery("/body".toList) }
		assertResult(Segment(KeyToken("body"),Empty)) { tokenizeQuery("body/".toList) }
		assertResult(Segment(KeyToken("body"),KeyToken("bodyz"))) { tokenizeQuery("body/bodyz".toList) }
		assertResult(Segment(KeyToken("body"),Segment(KeyToken("bodyz"),KeyToken("bodykz")))) { tokenizeQuery("body/bodyz/bodykz".toList) }
		assertResult(Slash(Segment(KeyToken("body"),Empty))) { tokenizeQuery("/body/".toList) }
		assertResult(Slash(Segment(KeyToken("body"),KeyToken("text")))) { tokenizeQuery("/body/text".toList) }
		assertResult(Slash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(KeyToken("txt2"),Empty))))) { tokenizeQuery("/txt/txt1/txt2/".toList) }
		assertResult(Slash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(ValueToken("txt/2"),Empty))))) { tokenizeQuery("/txt/txt1/${txt/2}/".toList) }
		assertResult(Slash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(KeyToken("${txt2\\}"),Empty))))) { tokenizeQuery("/txt/txt1/${txt2\\}/".toList) }
	}


}

import org.scalatest.FunSpec

class ActionTokenizerSpec extends FunSpec {

	import io.legs.utils.ActionTokenizer._

	it("tokenizes input correctly"){
		assertResult(StartSlash(Empty)) { tokenized("/".toList) }
		assertResult(Empty) { tokenized("".toList) }
		assertResult(StartSlash(KeyToken("body"))) { tokenized("/body".toList) }
		assertResult(Segment(KeyToken("body"),Empty)) { tokenized("body/".toList) }
		assertResult(Segment(KeyToken("body"),KeyToken("bodyz"))) { tokenized("body/bodyz".toList) }
		assertResult(Segment(KeyToken("body"),Segment(KeyToken("bodyz"),KeyToken("bodykz")))) { tokenized("body/bodyz/bodykz".toList) }
		assertResult(StartSlash(Segment(KeyToken("body"),Empty))) { tokenized("/body/".toList) }
		assertResult(StartSlash(Segment(KeyToken("body"),KeyToken("text")))) { tokenized("/body/text".toList) }
		assertResult(StartSlash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(KeyToken("txt2"),Empty))))) { tokenized("/txt/txt1/txt2/".toList) }
		assertResult(StartSlash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(ValueToken("txt/2"),Empty))))) { tokenized("/txt/txt1/${txt/2}/".toList) }
		assertResult(StartSlash(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(KeyToken("${txt2\\}"),Empty))))) { tokenized("/txt/txt1/${txt2\\}/".toList) }
		assertResult(Segment(KeyToken("MAP_PAR"),Segment(ValueToken("[1,2,3,4,5,6,7,8,9,10]"),Segment(ValueToken("\"num\""),KeyToken("instructionsP")))))
			{ tokenized("""MAP_PAR/${[1,2,3,4,5,6,7,8,9,10]}/${"num"}/instructionsP""".toList) }


	}


	it("gets a list of input tokens"){
		assertResult(List(KeyToken("txt"),KeyToken("txt1"),ValueToken("txt/2"))) { getInputs(Segment(KeyToken("txt"),Segment(KeyToken("txt1"),Segment(ValueToken("txt/2"),Empty)))) }
	}


}

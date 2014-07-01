import io.legs.Step
import org.scalatest.FunSpec

/**
 * Created: 6/27/13 2:30 PM
 */
class JSONParsingSpec extends FunSpec {

	it("parses simple json with action"){

		val action = "SOMETHING"
		val jsonStr = s"""[{ "action" : "$action", "values" : {} }]"""
		val output = Step.from(jsonStr)


		assertResult(1){output.length}
		assertResult(action){output.head.action}
	}



	val one =
		"""
		  |[
		  |{
		  |	"action": "io.legs.Scraper/EXTRACT_HTML_XPATH/fromParam/selector/validator",
		  |	"values" : {
		  |		"fromParam": "fromParam",
		  |		"selector": ".//a[@class=\"titlescat\"]/@href",
		  |		"validator" : ".*"
		  |	}
		  |}
		  |]
		""".stripMargin

	val two =
		"""
		  |[
		  |  { 
		  |		"action" : "io.legs.Scraper/EXTRACT_HTML_XPATH/#{data}/selector/validator",
		  |		"values" : {
		  |			"selector": ".//a[@class=\"titlescat\"]/@href",
		  |			"validator" : ".*"
		  |  	}
		  |  }
		  |]
		"""


}

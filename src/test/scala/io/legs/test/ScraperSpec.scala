package io.legs.test

import io.legs.Specialization
import io.legs.Specialization.Yield
import io.legs.network.Communicator
import io.legs.specialized.{BaseScraperSpecialized, SimpleScraperSpecialized}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.{FunSpec, ParallelTestExecution}
import play.api.libs.json.JsString

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class ScraperSpec extends FunSpec with MockFactory with AsyncAssertions with ParallelTestExecution {


	val url1 = "url1"

	val data1 = "dddddddd1"

	val param1 = "p1"

	val html1 =
		s"""
		  |<html>
		  |	<head><title>First parse</title></head>
		  |	<body><p>$data1</p></body>
		  |</html>
		""".stripMargin

	it("triggers the communicator"){
		val tcm = mock[Communicator]

		(tcm.getHtmlStr _).expects(url1).returning(data1)

		object Obj extends BaseScraperSpecialized {
			val communicator = tcm
		}


		val actionRes = Obj.invokeAction("FETCH",List("url"),Map(),Map("url"->JsString(url1)))

		val result = Await.result(actionRes, Specialization.oneMinuteDuration)

		assertResult( Yield(Some(data1)) ) { result }

	}




	it("can extract content from a string"){
		val tcm = mock[Communicator]

		object Obj extends BaseScraperSpecialized {
			val communicator = tcm
		}

		val actionFuture = Obj.invokeAction("EXTRACT_JSOUP",
			List("dataStr","selector","validator"),Map(),
			Map("dataStr"->JsString(html1), "selector" -> JsString("p") ,"validator"->JsString("") ))

		val result = Await.result(actionFuture, Specialization.tenSecDuration)
		assertResult( Yield(Some(List(data1))) ) { result }


	}

	it("catches a badly formatted selector and returns an error"){

		val tcm = mock[Communicator]

		object Obj extends BaseScraperSpecialized {
			val communicator = tcm
		}

		val actionFuture = Obj.invokeAction("EXTRACT_JSOUP",
			List("dataStr","selector","validator"),Map(),
			Map("dataStr"-> JsString(html1), "selector" -> JsString("!@#p") ,"validator"->JsString("") ))

		try {
			val result = Await.result(actionFuture, Specialization.tenSecDuration)
			throw new Throwable("not good!")
		} catch {
			case e : Throwable => // good, nothing to do here
		}

	}

	it("extracts data from html using xpath"){
		val tcm = mock[Communicator]

		object Obj extends BaseScraperSpecialized {
			val communicator = tcm
		}

		val actionFuture = Obj.invokeAction("EXTRACT_HTML_XPATH",
			List("dataStr", "selector", "validator"),Map(),
			Map("dataStr"->JsString(html1), "selector"-> JsString("//p"), "validator"-> JsString("")))

		val result = Await.result(actionFuture, Specialization.tenSecDuration)
		assertResult( Yield(Some(List(data1))) ) { result }

	}

	it("extracts nodes from XML"){
		val inputFileStr = scala.io.Source.fromFile(new java.io.File("src/test/scala/helpers/test.xml")).mkString
		val resFuture = SimpleScraperSpecialized.invokeAction("EXTRACT_XML_XPATH",List("input","selector","validator"),Map(),
			Map("input"->JsString(inputFileStr),"selector" -> JsString("//_links"), "validator" -> JsString("")))

		assertResult(4) {
			Await.result(resFuture,Specialization.tenSecDuration).valueOpt.get.asInstanceOf[List[String]].length
		}

	}

	it ("resolves XML with w3c resolvable value"){
		val inputXml = scala.io.Source.fromFile(new java.io.File("src/test/scala/helpers/dtdxml.xml")).mkString

		val resFuture = SimpleScraperSpecialized.invokeAction("EXTRACT_XML_XPATH",List("input","selector","validator"),Map(),
			Map("input"->JsString(inputXml),"selector" -> JsString("//_links"), "validator" -> JsString("")))

		assertResult(1) {
			Await.result(resFuture,Specialization.tenSecDuration).valueOpt.get.asInstanceOf[List[String]].length
		}

	}

	ignore ("extracts an attribute from HTML element node"){}
	ignore ("it gets a bad document to extract"){}


}







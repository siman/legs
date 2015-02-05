package io.legs.specialized

import java.io.StringReader
import javax.xml.transform.stream.StreamSource

import com.typesafe.scalalogging.Logger
import io.legs.Specialization
import io.legs.Specialization.{RoutableFuture, Yield}
import io.legs.documentation.Annotations.{LegsFunctionAnnotation, LegsParamAnnotation}
import io.legs.network.Communicator
import io.legs.network.simple.SimpleCommunicator
import net.sf.saxon.s9api.Processor
import org.htmlcleaner.{DomSerializer, HtmlCleaner}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory

import scala.collection.immutable.::
import scala.concurrent._


object SimpleScraper extends Scraper {
	val communicator = SimpleCommunicator
}


trait Scraper extends Specialization {

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	def communicator: Communicator


	@LegsFunctionAnnotation(
		details = "fetch HTML web resource, while fixing the underlying HTML then turn to String",
		yieldType = "String",
		yieldDetails = "returns the resource as string"
	)
	def FETCH(state: Specialization.State,
		url:String @LegsParamAnnotation("web url")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val res = communicator.getHtmlStr(url)
			Yield(Some(res))
		}

	@LegsFunctionAnnotation(
		details = "fetch raw resource",
		yieldType = "String",
		yieldDetails = "string value of fetched resource"
	)
	def FETCH_RAW(state: Specialization.State,
		url:String @LegsParamAnnotation("web url")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val res = communicator.getUrlStr(url)
			Yield(Some(res))
		}

	@LegsFunctionAnnotation(
		details = "Uses Jsoup selector syntax for extraction of values from structured HTML/XML" +
			"docs - docs - http://jsoup.org/cookbook/extracting-data/selector-syntax" +
			"playground - playground - http://try.jsoup.org/",
		yieldType = List.empty[String],
		yieldDetails = "list of matching values"
	)
	def EXTRACT_JSOUP(state: Specialization.State,
		inputString: String @LegsParamAnnotation("input HTML/XML as String"),
		selector: String @LegsParamAnnotation("JSOUP style selector"),
		validator: String @LegsParamAnnotation("a validation REGEX")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val parsed = Jsoup.parse(inputString)

			val resArr = parsed.select(selector).toArray
			val returns = resArr.map(_.asInstanceOf[Element].text()).toList
			Yield(Some(returns))

		}


	@LegsFunctionAnnotation(
		details = "extract values from correctly structured XML input value using XPATH selector",
		yieldType = List.empty[String],
		yieldDetails = "resulting list of values"
	)
	def EXTRACT_XML_XPATH(state: Specialization.State,
		inputString: String @LegsParamAnnotation("valid XML"),
		selector: String @LegsParamAnnotation("XPATH expression"),
		validator: String @LegsParamAnnotation("REGEX result validation")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {
			val processor = new Processor(false)
			val cleanInput = inputString.replace('&', ' ')
			val doc = processor.newDocumentBuilder().build(new StreamSource(new StringReader(cleanInput)))
			val xpCompiler = processor.newXPathCompiler()
			val xpath = xpCompiler.compile(selector)
			val loaded = xpath.load()
			loaded.setContextItem(doc)
			val evaluated = loaded.evaluate()

			import scala.collection.JavaConverters._
			val foundItems = evaluated.iterator.asScala.map(_.getStringValue).toList

			Yield(Some(foundItems))
		}

	@LegsFunctionAnnotation(
		details = "execute XPATH expression over valid HTML formatted string",
		yieldType = List.empty[String],
		yieldDetails = "list of matching values"
	)
 	def EXTRACT_HTML_XPATH(state: Specialization.State,
		inputString: String @LegsParamAnnotation("valid HTML"),
		selector: String @LegsParamAnnotation("XPATH expression"),
		validator: String @LegsParamAnnotation("REGEX result validation")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		Future {

			val cleaner = new HtmlCleaner()
			val properties = cleaner.getProperties
			val node = cleaner.clean(inputString)
			val domSerializer = new DomSerializer(properties)
			val dom = domSerializer.createDOM(node)
			val domSource =  new javax.xml.transform.dom.DOMSource(dom)

			val processor = new Processor(false)
			val doc = processor.newDocumentBuilder().build(domSource)
			val xpCompiler = processor.newXPathCompiler()
			val xpath = xpCompiler.compile(selector)
			val loaded = xpath.load()
			loaded.setContextItem(doc)
			val evaluated = loaded.evaluate()

			import scala.collection.JavaConverters._
			val foundItems = evaluated.iterator.asScala.map(_.getStringValue).toList

			Yield(Some(foundItems))
		}

	@LegsFunctionAnnotation(
		details = "execute XPATH expression over valid HTML formatted string",
		yieldType = "String",
		yieldDetails = "single matching value as String"
	)
	def EXTRACT_HTML_XPATH_FIRST(state: Specialization.State,
		inputString: String @LegsParamAnnotation("valid HTML"),
		selector: String @LegsParamAnnotation("XPATH expression"),
		validator: String @LegsParamAnnotation("REGEX result validation")
	)(implicit ctx : ExecutionContext) : RoutableFuture =
		EXTRACT_HTML_XPATH(state,inputString,selector,validator).map {
			case Yield(Some(x::xs)) => Yield(Some(x))
			case Yield(None) | Yield(Some(Nil)) => Yield(Some(""))
			case whatever => whatever
		}

}


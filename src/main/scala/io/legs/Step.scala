package io.legs

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json._

case class Step (
	action: String,
	values: Option[Map[String,JsValue]],
	yields: Option[String],
	transform : Option[JsArray] = None
)

object Step {

	implicit val fmt = Json.format[Step]

	val logger = Logger(LoggerFactory.getLogger(getClass))

	def from(jsonArray : JsArray) : List[Step] =
		jsonArray.value.toList.map(v=> fmt.reads(v).getOrElse(
			Step("ECHO/message",
				Some(Map("message" -> JsString("could not parse step from:" + v.toString() ))),None)
		))

	def from(jsonString : String) : List[Step] =
		Json.parse(jsonString) match {
			case v: JsArray => from(v)
			case o: JsObject=> from(JsArray(Seq(o)))
			case _=>
				logger.error("bad value passed for json parsing, it can only be JsObject or JsArray:" + jsonString)
				Nil
		}

}
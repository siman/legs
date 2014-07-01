package io.legs.utils

import play.api.libs.json._

object EnumJson {

	def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
		def reads(json: JsValue): JsResult[E#Value] = json match {
			case JsString(s) =>
				try {
					JsSuccess(enum.withName(s))
				} catch {
					case _: NoSuchElementException =>
						JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not contain '$s'")
				}

			case _ => JsError("String value expected")
		}
	}

	implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
		def writes(v: E#Value): JsValue = JsString(v.toString)
	}

	import scala.language.implicitConversions
	implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
		Format(enumReads(enum), enumWrites)
	}

	def toJsonMap[E <: Enumeration](enum: E) : JsValue =
		Json.toJson(enum.values.zip(enum.values).toMap.map(v=>(v._1.toString,v._2.toString)))
}

package io.nozzler

import upickle.{ReadWriter, Reader, Writer}


object UpickleImplicits {


	import play.api.libs.json._
	
	implicit def playJsonWR: Writer[JsValue] with Reader[JsValue] = ReadWriter[JsValue](unparshalPlayJson,unparshalUpicklePlayJson)

	private def unparshalPlayJson(v: JsValue) : upickle.Js.Value =
		v match {
			case JsNull => upickle.Js.Null
			case JsArray(values) => upickle.Js.Arr(values.map(unparshalPlayJson) : _*)
			case JsObject(values) => upickle.Js.Obj(values.map(v=> v._1 -> unparshalPlayJson(v._2)) : _*)
			case JsBoolean(value) => if (value) upickle.Js.True else upickle.Js.False
			case JsNumber(value) => upickle.Js.Num(value.toDouble)
			case JsString(value) => upickle.Js.Str(value)
			case _ : JsUndefined => upickle.Js.Null
		}

	private def unparshalUpicklePlayJson : PartialFunction[upickle.Js.Value,JsValue] =
		{
			case upickle.Js.Null => JsNull
			case upickle.Js.Arr(values @ _*) => JsArray(values.map(unparshalUpicklePlayJson))
			case upickle.Js.Obj(values @ _*) => JsObject(values.map(v=> v._1 -> unparshalUpicklePlayJson(v._2)))
			case upickle.Js.True => JsBoolean(value = true)
			case upickle.Js.False => JsBoolean(value = false)
			case upickle.Js.Num(value) => JsNumber(BigDecimal(value))
			case upickle.Js.Str(value) => JsString(value)
		}



}

package io.legs.utils

import play.api.libs.json._

object JsonFriend {

	def materialize(v:Any) =
		v match {
			case _v:JsString =>		_v.as[String]
			case _v:JsBoolean => 	_v.as[Boolean]
			case _v:JsNumber => 	_v.as[BigDecimal]
			case _v:JsUndefined => 	None
			case _v:JsNull.type => 	None
			case _v:JsArray => 		_v.value.toList
			case _ => 				v
		}

	def jsonify(v:Any) : JsValue =
		v match {
			case v:JsValue => v
			case v:String => JsString(v)
			case v:Boolean => JsBoolean(v)
			case v:BigDecimal =>JsNumber(v)
			case v:Double =>JsNumber(v)
			case v:Int =>JsNumber(v)
			case v:List[Any] => JsArray(v.map(jsonify))
			case v:Seq[Any] => JsArray(v.map(jsonify))
			case None => JsNull
			case _ => throw new Exception("could not jsonify value:" + v)
		}
}

package io.legs.utils

object ActionTokenizer {

	sealed trait QueryToken
	case object Empty extends QueryToken
	case class Slash(qt : QueryToken) extends QueryToken
	sealed trait InputTokens extends QueryToken
	case class KeyToken(s : String) extends InputTokens
	case class ValueToken(s : String) extends InputTokens
	case class Segment(left : InputTokens, right: QueryToken) extends QueryToken

	def tokenizeQuery(query : List[Char]) : QueryToken =
		tokenizeQueryImpl(query.reverse) //the query parsing happens in reverse..

	private val hasUnescapedCurly = """.+\{\$""".r

	/**
	 * TODO: add support for escaping slash charecter
	 * */
	private def tokenizeQueryImpl(query : List[Char], out : Option[QueryToken] = None) : QueryToken =
		query match {
			case Nil => out.getOrElse(Empty)
			case '/'::Nil => 			Slash(out.getOrElse(Empty))
			case '/'::xs => 			tokenizeQueryImpl(xs, Some(out.getOrElse(Empty)))
			case '}'::xs if xs.headOption != Some('\\') && hasUnescapedCurly.findFirstIn(xs.toIndexedSeq).isDefined =>
				val value = hasUnescapedCurly.findFirstIn(xs.toIndexedSeq).get.dropRight(2) // drop the $} stuff after extraction
				val leftOver = hasUnescapedCurly.split(xs.toIndexedSeq).tail.mkString // this is the left over text
				out match {
					case None => 		tokenizeQueryImpl(leftOver.toList, Some(ValueToken(value.reverse)))
					case Some(qt) => 	tokenizeQueryImpl(leftOver.toList, Some(Segment(ValueToken(value.reverse),qt)))
				}
			case xs if xs.contains('/') =>
				val split = xs.splitAt(xs.indexOf('/'))
				out match {
					case None => 		tokenizeQueryImpl(split._2, Some(KeyToken(split._1.reverse.mkString)))
					case Some(qt) => 	tokenizeQueryImpl(split._2, Some(Segment(KeyToken(split._1.reverse.mkString),qt)))
				}
			case xs =>
				out match {
					case None => 		KeyToken(xs.reverse.mkString)
					case Some(qt) => 	Segment(KeyToken(xs.reverse.mkString),qt)
				}
		}


}

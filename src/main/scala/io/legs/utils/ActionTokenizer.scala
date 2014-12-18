package io.legs.utils

object ActionTokenizer {

	sealed trait ActionToken
	case object Empty extends ActionToken
	case class StartSlash(qt : ActionToken) extends ActionToken
	sealed trait InputToken extends ActionToken { def s : String }
	case class KeyToken(s : String) extends InputToken
	case class ValueToken(s : String) extends InputToken
	case class Segment(left : InputToken, right: ActionToken) extends ActionToken

	def tokenizd(query : List[Char]) : ActionToken =
		tokenizeQueryImpl(query.reverse) //the query parsing happens in reverse..

	private val hasUnescapedCurly = """.+\{\$""".r

	/**
	 * TODO: add support for escaping slash charecter
	 * */
	private def tokenizeQueryImpl(query : List[Char], out : Option[ActionToken] = None) : ActionToken =
		query match {
			case Nil => out.getOrElse(Empty)
			case '/'::Nil => 			StartSlash(out.getOrElse(Empty))
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



	def getInputs(at: ActionToken) : List[InputToken] = getInputsImpl(at).reverse // appending is done in reverse

	private def getInputsImpl(at: ActionToken, out : List[InputToken] = Nil) : List[InputToken] =
		at match {
			case Empty if out.isEmpty => 	throw new Throwable("action can not be empty")
			case Empty => 					out
			case StartSlash(_) => 			throw new Throwable("action can not begin with a slash")
			case it : InputToken =>			it::out
			case Segment(left,right)=>  	getInputsImpl(right,left::out)
		}

}

package io.legs.runner

import com.typesafe.scalalogging.Logger
import io.legs.Worker
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object JsonFileRunner {

	val logger = Logger(LoggerFactory.getLogger(getClass))

	def main(args : Array[String]): Unit ={

		val help =
			"""How To Use:
			  |sbt runJson [<option> [argument]], [...]
			  |
			  | Options
			  | =======
			  | --file <full file path>
			""".stripMargin

		val input = parseArgs(args.toList)

		if (input.isEmpty){
			logger.info(help)
		} else if (input.contains("file") && input("file").isDefined) {
			logger.info("reading json file from:" + input("file").get)
			val jsonStr = scala.io.Source.fromFile(input("file").get,"UTF-8").mkString

			Worker.execute(jsonStr) match {
				case Success(v) => println("last yield:",v)
				case Failure(e) => println("failed executing json Str",e)
			}
		} else {
			logger.error("did not understand input:\"" + args.mkString(" ") + "\"")
			logger.info(help)
		}


	}

	private def parseArgs(args : List[String], found : Map[String,Option[String]] = Map.empty[String,Option[String]]) : Map[String,Option[String]] =
		args match {
			case arg::value::xs if arg.startsWith("--") && !value.startsWith("--") => parseArgs(xs, found + (arg.drop(2) -> Some(value)))
			case arg::xs if arg.startsWith("--") => parseArgs(xs, found + (arg.drop(2) -> None))
			case Nil => found
			case _ => throw new Throwable("unexpected input" + args.toString)
		}

}

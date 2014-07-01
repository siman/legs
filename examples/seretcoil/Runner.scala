package seretcoil

import io.legs.{Worker, Step}
import scala.util.{Failure, Success}
import io.legs.specialized.Queue
import io.legs.utils.InstructionsFileResolver


object Runner {


	def getMovies() {

		val json = scala.io.Source.fromFile("./jobs/seret_movies.json", "UTF-8").mkString

		Queue.setupRedis()

		Worker.execute(json) match {
			case Success(v) =>
				println(v)
//				println(v.valueOpt.get.asInstanceOf[List[String]].head)
			case Failure(e) => println(e)
		}

		println("<><><><><<><>")
	}

	def startWorkers(){



	}

	def main(args: Array[String]) {
//		getMovies()
		val foundOpt = InstructionsFileResolver.getFile("seret_movies")
		Worker.execute(foundOpt.get) match {
			case Success(v)=>
				println(v)
			case Failure(e)=> println(e)
		}
	}
}

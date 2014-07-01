package seretcoil

import io.legs.{Worker, Step}
import scala.util.{Failure, Success}
import io.legs.specialized.Queue
import io.legs.utils.InstructionsFileResolver


object Runner {

	def main(args: Array[String]) {
		val foundOpt = InstructionsFileResolver.getFile("wikipower")
		Worker.execute(foundOpt.get) match {
			case Success(v)=>
				println(v)
			case Failure(e)=> println(e)
		}
	}
}

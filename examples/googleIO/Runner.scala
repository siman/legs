package seretcoil

import io.legs.network.simple.SimpleCommunicator
import io.legs.utils.InstructionsFileResolver
import io.legs.{Coordinator, Worker}
import scala.util.{Failure, Success}


object Runner {

	def main(args: Array[String]) {
		val foundOpt = InstructionsFileResolver.getFile("gio14")
		Worker.execute(foundOpt.get,Map()) match {
			case Success(v)=> println(v,"done...")
				println(v)
			case Failure(e)=> println(e)
		}
	}
}

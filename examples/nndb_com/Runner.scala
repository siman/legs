package seretcoil

import io.legs.network.simple.SimpleCommunicator
import io.legs.utils.InstructionsFileResolver
import io.legs.{Coordinator, Worker}
import scala.util.{Failure, Success}


object Runner {

	def main(args: Array[String]) {
		val foundOpt = InstructionsFileResolver.getFile("graph_walker")
		Worker.execute(foundOpt.get,Map("inputNodeId"-> "124837")) match {
			case Success(v)=> println(v,"done...")
				Coordinator.start(List("nndb"),6)
			case Failure(e)=> println(e)
		}

//		SimpleScraper.



	}
}

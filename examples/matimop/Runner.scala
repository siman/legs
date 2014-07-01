package seretcoil

import io.legs.Coordinator


object Runner {

	def main(args: Array[String]) {
		Coordinator.start(List("matimop"),6)
	}
}

package io.legs.ui.client

import scala.scalajs.js.JSApp
import io.legs.ui.shared.Shared

object Tut extends JSApp {
	def main(): Unit = {
		val shared = Shared("stringy",123)
		println("Hello world!",shared)

	}
}


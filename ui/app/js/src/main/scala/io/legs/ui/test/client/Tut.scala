package io.legs.ui.test.client

import io.legs.ui.test.shared.Shared

import scala.scalajs.js.JSApp

object Tut extends JSApp {
	def main(): Unit = {
		val shared = Shared("stringy",123)
		println("Hello world!",shared)

	}
}


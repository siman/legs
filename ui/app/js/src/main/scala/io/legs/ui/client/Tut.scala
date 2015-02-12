package io.legs.ui.client

import io.legs.ui.test.shared.Shared

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport
object Tut extends JSApp {
	@JSExport
	def main(): Unit = {
		val shared = Shared("stringy",909090)
		println("woohoooo",shared)
	}
}


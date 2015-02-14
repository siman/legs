package io.legs.ui.client.components

import autowire._
import io.legs.ui.shared.Api
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import io.legs.ui.client.components.Bootstrap._
import io.legs.ui.client.services.AjaxClient


import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * This is a simple component demonstrating how to interact with the server
 */
object Motd {

	case class State(message: String)

	class Backend(t: BackendScope[Unit, State]) {
		def refresh() {
			// load a new message from the server
			AjaxClient[Api].motd("User X").call().foreach { message =>
				t.modState(_ => State(message))
			}
		}
	}

	// create the React component for holding the Message of the Day
	val Motd = ReactComponentB[Unit]("Motd")
		.initialState(State("loading...")) // show a loading text while message is being fetched from the server
		.backend(new Backend(_))
		.render((_, S, B) => {
			Panel(PanelProps("Message of the day"), div(S.message),
				Button(ButtonProps(B.refresh, CommonStyle.danger), Icon.refresh, span(" Update"))
			)
		})
		.componentDidMount(scope => {
			scope.backend.refresh()
		})
		.buildU

	def apply() = Motd()
}

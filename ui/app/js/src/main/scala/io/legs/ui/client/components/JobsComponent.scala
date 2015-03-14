package io.legs.ui.client.components

import io.legs.ui.client.components.Bootstrap._
import io.legs.ui.client.services.NozzleClientBackend
import io.legs.ui.shared.model.ScheduledJob
import io.nozzler.NozzleService
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object JobsComponent {

	implicit val backend = NozzleClientBackend
	import io.legs.ui.shared.util.PicklingImplicits._

	import NozzleService._

	case class State(message: String)

	class Backend(t: BackendScope[Unit, State]) {

		def refresh() {

			//load a new message from the server
			read[ScheduledJob](None,Map.empty[String,String]).map {
				case Nil => t.modState(_ => State("loading...1"))
				case res :: Nil => t.modState(_ => State(res.jobId))
			}
		}
	}

	// create the React component for holding the Message of the Day
	val JobsComponent = ReactComponentB[Unit]("Jobs")
		.initialState(State("loading...")) // show a loading text while message is being fetched from the server
		.backend(new Backend(_))
		.render((_, S, B) => {
			Panel(PanelProps("Message of the day"), div(S.message),
				Button(ButtonProps(B.refresh, CommonStyle.danger), Icon.refresh, span("Update"))
			)
		})
		.componentDidMount(scope => {
			scope.backend.refresh()
		})
		.buildU

	def apply() = JobsComponent()
}

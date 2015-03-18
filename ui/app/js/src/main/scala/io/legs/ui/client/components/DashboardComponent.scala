package io.legs.ui.client.components

import io.legs.ui.client.router.MainRouter
import io.legs.ui.client.services.NozzleClientBackend
import io.legs.ui.shared.model.ScheduledJob
import io.nozzler.NozzleService
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import thirdparty.Bootstrap._
import thirdparty.Icon

object DashboardComponent {

	import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
	implicit val backend = NozzleClientBackend
	import NozzleService._
	import io.legs.ui.shared.util.PicklingImplicits._

	val component = ReactComponentB[MainRouter.Router]("Dashboard")
		.render(router => {
			// get internal links
			<.div(
				// header, MessageOfTheDay and chart components
				<.h2("Dashboard"),
				<.div(jobs()),
				// create a link to the Todo view
				<.div(router.link(MainRouter.todoLoc)("Check the job queue!"))
			)
		}).build



	private class Backend(t: BackendScope[Unit, String]) {

		def refresh() {
			//load a new message from the server
			read[ScheduledJob](None,Map.empty[String,String]).map {
				case Nil => t.modState(_ => "loading...1")
				case res :: xs => t.modState(_ => res.jobId)
			}
		}

	}

	private lazy val jobs =
		ReactComponentB[Unit]("Jobs")
			.initialState("loading...") // show a loading text while message is being fetched from the server
			.backend(new Backend(_))
			.render((_, S, B) => {
				Panel(PanelProps("Refresh"), <.div(S),
					Button(ButtonProps(B.refresh, CommonStyle.danger), Icon.refresh, <.span("Update"))
				)
			})
			.componentDidMount(scope => {
				scope.backend.refresh()
			})
			.buildU

}

package io.legs.ui.client.components

import io.legs.ui.client.router.MainRouter
import io.legs.ui.client.services.NozzleClientBackend
import io.legs.ui.shared.model.ScheduledJob
import io.nozzler.NozzleService
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import thirdparty.Bootstrap._
import thirdparty.Icon
import NozzleService._

object DashboardComponent {

	import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
	implicit val backend = NozzleClientBackend
	import NozzleService._
	import io.legs.ui.shared.util.PicklingImplicits._

	case class DashboardState(numQueued : Int)

	val component = ReactComponentB[MainRouter.Router]("Dashboard")
		.render(router => {
			<.div(
				<.h2("_"),
				<.div(jobs()),
				<.div(router.link(MainRouter.todoLoc)("Check the job queue!"))
			)
		}).build



	private class Backend(t: BackendScope[Unit, DashboardState]) {

		def refresh() {

			read[ScheduledJob](None,Map()).map {
				case xs => t.modState(_ => DashboardState(numQueued = xs.length))
			}

		}

	}

	private lazy val jobs =
		ReactComponentB[Unit]("Jobs")
			.initialState(DashboardState(numQueued = -1))
			.backend(new Backend(_))
			.render((_, S, B) => {
				Panel(PanelProps("Status"),
					<.div(s"Scheduled #${S.numQueued}"),
					Button(ButtonProps(B.refresh, CommonStyle.danger), Icon.refresh,
						<.span("Update")
					)
				)
			})
			.componentDidMount(scope => {
				scope.backend.refresh()
			})
			.buildU

}

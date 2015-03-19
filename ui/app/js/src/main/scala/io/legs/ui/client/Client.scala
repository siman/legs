package io.legs.ui.client


import io.legs.ui.client.components.NavigationComponent
import NavigationComponent.MenuItem
import io.legs.ui.client.router.MainRouter
import io.legs.ui.client.services.NozzleClientBackend
import japgolly.scalajs.react.extra.router.{BaseUrl, Router}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{React, ReactComponentB}
import org.scalajs.dom
import thirdparty.Icon

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport


@JSExport
object Client extends JSApp {

	import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
	import io.legs.ui.shared.util.PicklingImplicits._

	val menuItems = List(
		MenuItem("Dashboard", Icon.dashboard, MainRouter.dashboardLoc,true),
		MenuItem("Schedules", Icon.check, MainRouter.scheduledLoc)
	)

	@JSExport
	def main(): Unit = {
		implicit val backend = NozzleClientBackend

		MainRouter.router

		React.render(RootComponent(),dom.document.body)
	}

	val RootComponent = ReactComponentB[Unit]("Root")
		.initialState(menuItems)
		.render((_,S) =>
			<.div(
				<.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
					<.div(^.className := "container")(
						<.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Tutorial")),
						<.div(^.className := "collapse navbar-collapse")(
							NavigationComponent.component((S, MainRouter.router))
						)
					)
				),
				<.div(^.className := "container")(MainRouter.routerComponent())
			)
		)
		.componentDidMount(scope =>
			MainRouter.listen(loc=>
				scope.modState(state => {
					state.map(i => i.copy(isActive = i.location == loc))
				})
			)
		)
		.buildU


}




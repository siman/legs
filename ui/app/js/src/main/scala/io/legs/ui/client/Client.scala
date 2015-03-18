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
		MenuItem("Todo", Icon.check, MainRouter.todoLoc)
	)

	@JSExport
	def main(): Unit = {

		val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
		val router  = MainRouter.routingEngine(baseUrl)

		implicit val backend = NozzleClientBackend

		React.render(RootComponent(router),dom.document.body)
	}

	val RootComponent = ReactComponentB[MainRouter.Router]("Root")
		.render(router =>
			<.div(
				<.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
					<.div(^.className := "container")(
						<.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Tutorial")),
						<.div(^.className := "collapse navbar-collapse")(
							NavigationComponent.component((menuItems,router))
						)
					)
				),
				<.div(^.className := "container")(Router.component(router)())
			)
		).build


}




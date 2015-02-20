package io.legs.ui.client

import io.legs.ui.client.components.Icon
import io.legs.ui.client.modules.MainMenu.MenuItem
import io.legs.ui.client.modules.{MainMenu, MainRouter}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, React}
import japgolly.scalajs.react.extra.router.{Router, BaseUrl}
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport



@JSExport
object Client extends JSApp {

	val menuItems = Seq(
		MenuItem("Dashboard", Icon.dashboard, MainRouter.dashboardLoc,true),
		MenuItem("Todo", Icon.check, MainRouter.todoLoc)
	)

	@JSExport
	def main(): Unit = {
		// build a baseUrl, this method works for both local and server addresses (assuming you use #)
		val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
		val router  = MainRouter.routingEngine(baseUrl)
		// tell React to render the router in the document body
		React.render(RootComponent(router),dom.document.body)
	}

	val RootComponent = ReactComponentB[MainRouter.Router]("Root")
		.render(router =>
			<.div(
				<.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
					<.div(^.className := "container")(
						<.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Tutorial")),
						<.div(^.className := "collapse navbar-collapse")(
							menuItems.map(mi=>
								MainMenu.MainMenuItem((mi,router))
							)
						)
					)
				),
//				currently active module is shown in this container
				<.div(^.className := "container")(Router.component(router)())
			)
		).build


}




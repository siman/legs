package io.legs.ui.client.router

import io.legs.ui.client.components.{DashboardComponent, QueueComponent}
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.extra.router._
import org.scalajs.dom

object MainRouter extends RoutingRules {
	// register the components and store locations
	val dashboardLoc = register(rootLocation(DashboardComponent.component))
	val scheduledLoc = register(location("#queue", QueueComponent.component))

	var activeLoc = Option.empty[MainRouter.Loc]
	private var listener = Option.empty[MainRouter.Loc => Unit]

	def listen(f : MainRouter.Loc => Unit) = listener = Some(f)

	onRouteChange(r => {
		activeLoc = Some(r)
		listener.foreach(_(r))
	})

	val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
	val router = MainRouter.routingEngine(baseUrl)
	val routerComponent = Router.component(router)

	// redirect all invalid routes to dashboard
	override protected val notFound = redirect(dashboardLoc, Redirect.Replace)

}

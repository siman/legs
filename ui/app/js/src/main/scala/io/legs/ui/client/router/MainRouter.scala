package io.legs.ui.client.router

import io.legs.ui.client.components.{DashboardComponent, QueueComponent}
import japgolly.scalajs.react.extra.router._

object MainRouter extends RoutingRules {
	// register the components and store locations
	val dashboardLoc = register(rootLocation(DashboardComponent.component))
	val todoLoc = register(location("#queue", QueueComponent.component))

	// redirect all invalid routes to dashboard
	override protected val notFound = redirect(dashboardLoc, Redirect.Replace)

}

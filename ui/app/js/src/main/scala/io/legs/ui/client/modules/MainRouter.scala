package io.legs.ui.client.modules

import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._

object MainRouter extends RoutingRules {
	// register the components and store locations
	val dashboardLoc = register(rootLocation(Dashboard.component))
	val todoLoc = register(location("#todo", ToDo.component))

	// redirect all invalid routes to dashboard
	override protected val notFound = redirect(dashboardLoc, Redirect.Replace)

}

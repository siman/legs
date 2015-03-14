package io.legs.ui.client.modules

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import io.legs.ui.client.components._

object Dashboard {
	// create the React component for Dashboard
	val component = ReactComponentB[MainRouter.Router]("Dashboard")
		.render(router => {
		// get internal links
		<.div(
			// header, MessageOfTheDay and chart components
			<.h2("Dashboard"),
			// create a link to the Todo view
			<.div(router.link(MainRouter.todoLoc)("Check your todos!"))
		)
	}).build
}

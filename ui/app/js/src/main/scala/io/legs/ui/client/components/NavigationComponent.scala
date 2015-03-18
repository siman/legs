package io.legs.ui.client.components

import io.legs.ui.client.router.MainRouter
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import thirdparty.Icon._



object NavigationComponent {

	case class MenuItem(label: String, icon: Icon, location: MainRouter.Loc, isActive : Boolean = false)

	protected val MainMenuItem = ReactComponentB[(MenuItem,MainRouter.Router)]("MainMenuItem")
		.render { P=>
			val (menuItem, router) = P
			<.li(menuItem.isActive ?= (^.className := "active"),
				router.link(menuItem.location)(menuItem.icon, " ", menuItem.label)
			)

		}
		.build

	def component =
		ReactComponentB[(List[MenuItem],MainRouter.Router)]("MainMenu")
			.render( P => {
				val (items, router) = P
				<.ul(^.className := "nav navbar-nav")(
					// build a list of menu items
					items.map { item => MainMenuItem((item,router)) }
				)
			})
			.build


}

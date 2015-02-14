package io.legs.ui.client.modules

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import io.legs.ui.client.components.Icon._
import io.legs.ui.client.components._

object MainMenu {

  case class MenuProps(activeLocation: MainRouter.Loc, router: MainRouter.Router)

  case class MenuItem(label: String, icon: Icon, location: MainRouter.Loc)

  val menuItems = Seq(
    MenuItem("Dashboard", Icon.dashboard, MainRouter.dashboardLoc),
    MenuItem("Todo", Icon.check, MainRouter.todoLoc)
  )

  val MainMenu = ReactComponentB[MenuProps]("MainMenu")
    .render(P => {
    <.ul(^.className := "nav navbar-nav")(
      // build a list of menu items
      for (item <- menuItems) yield {
        <.li((P.activeLocation == item.location) ?= (^.className := "active"),
          P.router.link(item.location)(item.icon, " ", item.label)
        )
      }
    )
  })
    .build

  def apply(props: MenuProps) = MainMenu(props)
}

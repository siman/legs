package io.legs.ui.client.modules

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.Router
import japgolly.scalajs.react.vdom.prefix_<^._
import io.legs.ui.client.components.Icon._
import io.legs.ui.client.components._



object MainMenu {


	case class MenuProps(activeLocation: MainRouter.Loc, router: MainRouter.Router)

	case class MenuItem(label: String, icon: Icon, location: MainRouter.Loc, isActive : Boolean = false)

	val MainMenuItem = ReactComponentB[(MenuItem,MainRouter.Router)]("MainMenuItem")
		.render { P=>
			val (menuItem, router) = P
			<.li(menuItem.isActive ?= (^.className := "active"),
				router.link(menuItem.location)(menuItem.icon, " ", menuItem.label)
			)

		}
		.build
//
//	val MainMenu = ReactComponentB[(List[MenuItem],Router[_])]("MainMenu")
//		.render(P => {
//			<.ul(^.className := "nav navbar-nav")(
//				// build a list of menu items
//				val (items, router) = P
//				items.map { item=>
//					<.li(item.isActive ?= (^.className := "active"),
//						router.link(item.location)(item.icon, " ", item.label)
//					)
//				}
//			)
//		})
//		.build

//
//
//	val MainMenu2 = ReactComponentB[(List[MenuItem],Router[_])]("ProductTable")
//		.render(P => {
//		val (menuItems, state) = P
//		val rows = menuItems
//			.flatMap{ menuItem =>
//			ProductCategoryRow.withKey(cat)(cat) :: ps.map(p => ProductRow.withKey(p.name)(p))
//		}
//		<.table(
//			<.thead(
//				<.tr(
//					<.th("Name"),
//					<.th("Price"))),
//			<.tbody(
//				rows))
//	})
//		.build



	case class Product(name: String, price: Double, category: String, stocked: Boolean)

	case class State(filterText: String, inStockOnly: Boolean)

	class Backend($: BackendScope[_, State])  {
		def onTextChange(e: ReactEventI) =
			$.modState(_.copy(filterText = e.target.value))
		def onCheckBox(e: ReactEvent) =
			$.modState(s => s.copy(inStockOnly = !s.inStockOnly))
	}

	val ProductCategoryRow = ReactComponentB[String]("ProductCategoryRow")
		.render(category => <.tr(<.th(^.colSpan := 2, category)))
		.build

	val ProductRow = ReactComponentB[Product]("ProductRow")
		.render(P =>
		<.tr(
			<.td(<.span(!P.stocked ?= ^.color.red, P.name)),
			<.td(P.price))
		)
		.build



	def productFilter(s: State)(p: Product): Boolean =
		p.name.contains(s.filterText) &&
			(!s.inStockOnly || p.stocked)

	val ProductTable = ReactComponentB[(List[Product], State)]("ProductTable")
		.render(P => {
		val (products, state) = P
		val rows = products.filter(productFilter(state))
			.groupBy(_.category).toList
			.flatMap{ case (cat, ps) =>
			ProductCategoryRow.withKey(cat)(cat) :: ps.map(p => ProductRow.withKey(p.name)(p))
		}
		<.table(
			<.thead(
				<.tr(
					<.th("Name"),
					<.th("Price"))),
			<.tbody(
				rows))
	})
		.build

	val SearchBar = ReactComponentB[(State, Backend)]("SearchBar")
		.render(P => {
		val (s, b) = P
		<.form(
			<.input(
				^.placeholder := "Search Bar ...",
				^.value       := s.filterText,
				^.onChange   ==> b.onTextChange),
			<.p(
				<.input(
					^.tpe     := "checkbox",
					^.onClick ==> b.onCheckBox,
					"Only show products in stock")))
	})
		.build

	val FilterableProductTable = ReactComponentB[List[Product]]("FilterableProductTable")
		.initialState(State("", false))
		.backend(new Backend(_))
		.render((P, S, B) =>
		<.div(
			SearchBar((S,B)),
			ProductTable((P,S)))
		).build

	val products = List(
		Product("FootBall", 49.99, "Sporting Goods", true),
		Product("Baseball", 9.99, "Sporting Goods", true),
		Product("basketball", 29.99, "Sporting Goods", false),
		Product("ipod touch", 99.99, "Electronics", true),
		Product("iphone 5", 499.99, "Electronics", true),
		Product("Nexus 7", 199.99, "Electronics", true))

//	React.render(FilterableProductTable(products), mountNode)

}

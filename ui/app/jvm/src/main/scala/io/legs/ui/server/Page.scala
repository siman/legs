package io.legs.ui.server

import scalatags.Text.all._
import scalatags.Text.tags2.{style => style2}

object Page{

	val boot = 	"io.legs.ui.client.Client().main()"

	object v {
		val bootstrap = "3.3.2"
		val fontAwesome = "4.3.2"
		val react = "0.12.2"
		val jQuery = "2.1.3"
	}

	val skeleton =
		"<!DOCTYPE html>" + html(
			head(
				meta(content := "text/html;charset=utf-8", httpEquiv := "Content-Type"),
				meta(content := "utf-8", httpEquiv := "encoding"),
				link(
					href := s"http://maxcdn.bootstrapcdn.com/bootstrap/${v.bootstrap}/css/bootstrap.min.css",
					rel := "stylesheet"
				),
				link(
					href := s"http://maxcdn.bootstrapcdn.com/font-awesome/${v.fontAwesome}/css/font-awesome.min.css",
					rel := "stylesheet"
				),
				script( src := s"//cdnjs.cloudflare.com/ajax/libs/react/${v.react}/react-with-addons.min.js" ),
				script( src := s"https://code.jquery.com/jquery-${v.jQuery}.min.js" ),
				script( src := s"http://maxcdn.bootstrapcdn.com/bootstrap/${v.bootstrap}/js/bootstrap.min.js" ),
				script( src := "/js/shared-fastopt.js" ),
				meta( name := "viewport", content := "width=device-width, initial-scale=1" ),
				style2(
					"""
					  |body { background-color: #fff; }
					  |
					""".stripMargin
				)
			),
			body(
				onload := boot,
				div(id := "body-container", cls := "container-fluid")
			)
		).render
}

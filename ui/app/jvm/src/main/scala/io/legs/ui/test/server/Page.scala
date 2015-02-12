package io.legs.ui.test.server

import scalatags.Text.all._
import scalatags.Text.tags2.{style => style2}

object Page{

	val boot = 	"io.legs.ui.client.Tut().main()"

	val skeleton =
		html(
			head(
				link(
					href := "http://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css",
					rel := "stylesheet"
				),
				link(
					href := "http://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css",
					rel := "stylesheet"
				),
				script( src := "https://code.jquery.com/jquery-2.1.3.min.js" ),
				script( src := "http://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js" ),
				script( src := "/js/appjs-fastopt.js" ),
				meta( name := "viewport", content := "width=device-width, initial-scale=1" ),
				style2(
					"""
					  |body { background-color : black; }
					""".stripMargin
				)
			),
			body(
				onload := boot,
				div(id := "container")
			)
		)
}

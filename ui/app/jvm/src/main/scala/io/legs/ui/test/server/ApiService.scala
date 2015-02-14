package io.legs.ui.test.server

import java.util.Date

import io.legs.ui.shared._

class ApiService extends Api {
	override def motd(name: String): String = s"Welcome to SPA, $name! Time is now ${new Date}"
	override def getTodos(): Seq[TodoItem] = {
		// provide some fake Todos
		Seq(
			TodoItem("1", "Wear shirt that says “Life”. Hand out lemons on street corner.", TodoLow, completed = false),
			TodoItem("2", "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, completed = false),
			TodoItem("3", "Walk away slowly from an explosion without looking back.", TodoHigh, completed = false),
			TodoItem("4", "Sneeze in front of the pope. Get blessed.", TodoNormal, completed = true)
		)
	}
	// update a Todo
	override def updateTodo(item: TodoItem): Unit = {
		// TODO, update database etc :)
		println(s"Todo item was updated: $item")
	}
}


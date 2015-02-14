package io.legs.ui.shared

trait Api {
	// message of the day
	def motd(name:String) : String
	// get Todo items
	def getTodos() : Seq[TodoItem]
	// update a Todo
	def updateTodo(item:TodoItem)
}

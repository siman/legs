package io.legs.ui.shared

import io.legs.ui.shared.model.ScheduledJob

import scala.concurrent.Future


trait Api {
	// message of the day
	def motd(name:String) : String
	// get Todo items
	def getTodos() : Seq[TodoItem]
	// update a Todo
	def updateTodo(item:TodoItem)

//	def getScheduledJobs() : Future[Seq[ScheduledJob]]

}

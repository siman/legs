package io.legs.ui.client.components

import io.legs.library.{JobStatus, Priority}
import io.legs.ui.shared.model.ScheduledJob
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object TodoList {

	case class TodoListProps(items: Seq[ScheduledJob], stateChange: (ScheduledJob) => Unit)

	val TodoList = ReactComponentB[TodoListProps]("TodoList")
		.render(P => {
			def renderItem(item: ScheduledJob) = {
				// convert priority into Bootstrap style
				val priority = item.jobData.priority match {
					case Priority.LOW => "list-group-item-info"
					case Priority.HIGH => "list-group-item-danger"
					case _ => ""
				}
				<.li(^.className := s"list-group-item $priority")(
					<.input(^.tpe := "checkbox", ^.checked := item.jobData.status == JobStatus.DONE, ^.onChange --> P.stateChange(item.copy(jobData = item.jobData.copy(status = JobStatus.DONE))),
						if (item.jobData.status == JobStatus.DONE) <.s(item.jobId) else <.span(item.jobId)
					))
			}
			<.ul(^.className := "list-group")(P.items map renderItem)
		})
		.build

	def apply(props: TodoListProps) = TodoList(props)
}

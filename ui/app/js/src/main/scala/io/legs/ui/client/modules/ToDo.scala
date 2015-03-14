package io.legs.ui.client.modules

import io.legs.ui.client.components.Bootstrap._
import io.legs.ui.client.components.TodoList.TodoListProps
import io.legs.ui.client.components._
import io.legs.ui.client.services.NozzleClientBackend
import io.legs.ui.shared.model.{ScheduledJob, JobLike}
import io.nozzler.NozzleService
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object ToDo {

	import io.legs.ui.shared.util.PicklingImplicits._
	implicit val backend = NozzleClientBackend

	case class TodoState(items: Seq[ScheduledJob])

	class Backend(t: BackendScope[_, TodoState]) {
		def updateTodo(item: ScheduledJob): Unit = {
			println("update..")
			t.modState(s => TodoState(s.items.map(i => if (i.jobId == item.jobId) item else i)))
			// update the state with the new TodoItem
			// inform the server about this update
			println("updating",item)
			//      AjaxClient[Api].updateTodo(item).call()
		}

		def refresh(): Unit = {

			println("called refresh!")
			NozzleService.read[ScheduledJob](None,Map()).map {
				case Nil => println("nothing")
				case xs =>
					println("!!!",xs)
					t.modState(_ => TodoState(xs))
			}
		}
	}

	// create the React component for ToDo management
	val component = ReactComponentB[MainRouter.Router]("TODO")
		.initialState(TodoState(Seq())) // initial state is an empty list
		.backend(new Backend(_))
		.render((router, S, B) => {
			println("rendering")
			Panel(PanelProps("What needs to be done"),
				TodoList(TodoListProps(S.items, B.updateTodo)))
		})
		.componentDidMount { c =>
			println("it did!")
			c.backend.refresh()
		}
		.build
}

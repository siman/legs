package io.legs.ui.client.components

import io.legs.library.{JobStatus, Priority}
import io.legs.ui.client.router.MainRouter
import io.legs.ui.client.services.NozzleClientBackend
import io.legs.ui.shared.model.ScheduledJob
import io.nozzler.NozzleService
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import thirdparty.Bootstrap._

object QueueComponent {

	import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
	implicit val backend = NozzleClientBackend
	import io.legs.ui.shared.util.PicklingImplicits._

	protected case class QueueState(items: List[ScheduledJob])

	protected class Backend(t: BackendScope[MainRouter.Router, QueueState]) {
		def updateQueue(item: ScheduledJob): Unit = {
			println("update..")
			t.modState(s => QueueState(s.items.map(i => if (i.jobId == item.jobId) item else i)))
			// update the state with the new TodoItem
			// inform the server about this update
			println("updating",item)
			//      AjaxClient[Api].updateTodo(item).call()
		}

		def refresh(): Unit = {
			NozzleService.read[ScheduledJob](None,Map()).map {
				case xs => t.modState(_ => QueueState(xs))
			}
		}
	}

	val component =
		ReactComponentB[MainRouter.Router]("QUEUE")
			.initialState(QueueState(List.empty[ScheduledJob])) // initial state is an empty list
			.backend(new Backend(_))
			.render((router, S, B) =>
				Panel(PanelProps("What needs to be done"),queueItems((S.items, B.updateQueue)))
			)
			.componentDidMount { c =>
				c.backend.refresh()
			}
			.build

	private def renderItem(item: ScheduledJob) = {
		// convert priority into Bootstrap style
		val priority = item.jobData.priority match {
			case Priority.LOW => "list-group-item-info"
			case Priority.HIGH => "list-group-item-danger"
			case _ => ""
		}
		<.li(^.className := s"list-group-item $priority")(
			<.input(^.tpe := "checkbox", ^.checked := item.jobData.status == JobStatus.DONE, ^.disabled := "disabled"),
			<.span(item.jobId),
			<.span(item.jobData.status.toString),
			<.span(item.schedule),
			<.span(item.jobData.instructions),
			<.span(item.jobData.description)
		)
	}

	private def queueItems =
		ReactComponentB[(Seq[ScheduledJob], (ScheduledJob) => Unit)]("QueueItems")
			.render(P => {
				<.ul(^.className := "list-group")(P._1 map renderItem)
			})
			.build

}


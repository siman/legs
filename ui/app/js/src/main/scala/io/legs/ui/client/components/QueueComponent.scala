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
			t.modState(s => QueueState(s.items.map(i => if (i.jobId == item.jobId) item else i)))
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
		<.tr(
			<.th(^.scope := "row",item.jobId),
			<.td(<.input(^.tpe := "checkbox", ^.checked := item.jobData.status == JobStatus.DONE, ^.disabled := "disabled")),
			<.td(item.jobData.status.toString),
			<.td(item.schedule),
			<.td(item.jobData.instructions),
			<.td(item.jobData.description)
		)

	}

	private def queueItems =
		ReactComponentB[(Seq[ScheduledJob], (ScheduledJob) => Unit)]("QueueItems")
			.render(P => {
				<.table(^.className := "table")(
					<.thead(
						<.tr(
							<.th("Job ID"),
							<.th("Done?"),
							<.th("Status"),
							<.th("Schedule"),
							<.th("Instructions"),
							<.th("Description")
						)
					),
					<.tbody(
						P._1 map renderItem
					)
				)
			})
			.build

}


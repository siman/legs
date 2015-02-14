package io.legs.ui.client.modules

import autowire._
import io.legs.ui.shared.{Api, TodoItem}
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import io.legs.ui.client.components.Bootstrap._
import io.legs.ui.client.components.TodoList.TodoListProps
import io.legs.ui.client.components._
import io.legs.ui.client.services.AjaxClient


import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object ToDo {

  case class TodoState(items: Seq[TodoItem])

  class Backend(t: BackendScope[_, TodoState]) {
    def updateTodo(item: TodoItem): Unit = {
      // update the state with the new TodoItem
      t.modState(s => TodoState(s.items.map(i => if (i.id == item.id) item else i)))
      // inform the server about this update
      AjaxClient[Api].updateTodo(item).call()
    }

    def refresh(): Unit = {
      // load Todos from the server
      AjaxClient[Api].getTodos().call().foreach { todos =>
        t.modState(_ => TodoState(todos))
      }
    }
  }

  // create the React component for ToDo management
  val component = ReactComponentB[MainRouter.Router]("TODO")
    .initialState(TodoState(Seq())) // initial state is an empty list
    .backend(new Backend(_))
    .render((router, S, B) => {
    Panel(PanelProps("What needs to be done"),
      TodoList(TodoListProps(S.items, B.updateTodo)))
  })
    .componentDidMount(_.backend.refresh())
    .build
}

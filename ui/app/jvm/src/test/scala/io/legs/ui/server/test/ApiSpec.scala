package io.legs.ui.server.test

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import io.legs.io.test.helpers.TestCRUDService
import io.legs.ui.server.Server
import io.nozzler.{NozzleBackend, NozzleService}
import org.scalatest._
import play.api.libs.json.Json
import spray.http.HttpMethods._
import spray.http.{HttpRequest, HttpResponse, Uri}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ApiSpec extends TestKit(ActorSystem("testsystem", ConfigFactory.parseString("""akka.event-handlers = ["akka.testkit.TestEventListener"]""")))
	with FunSpecLike with ImplicitSender with BeforeAndAfter  {

	implicit val ec = system.dispatcher

	var zServer : ActorRef = _

	val duration = Duration(2, "seconds")

	def toBlocking[T](future : Future[T]) : T = Await.result(future, duration)

	it("uses the API directly and works") {

		zServer = system.actorOf(Props(new Server(TestCRUDService :: Nil)))

		zServer ! HttpRequest(GET,Uri("/api/testcrudservice"))

		fishForMessage(10.seconds) {
			case HttpResponse(code, entity, _, _) if code.intValue == 200 =>
				println(entity)
				true
			case badMessage =>
				println("recieved bad message:" + badMessage.toString)
				false
		}


	}


	it("uses the sugared macro impl"){

		implicit val backend = new NozzleBackend {
			override def processMessage(resource: String, method: String, model: Option[String], uid: Option[String], query: Map[String, String]): Future[Option[String]] =
				Future.successful(Some(Json.toJson(TestCRUDService("good!") :: Nil).toString()))
		}

		assertResult(TestCRUDService("good!") :: Nil){
			toBlocking(NozzleService.read[TestCRUDService](None,Map()))
		}
	}


}



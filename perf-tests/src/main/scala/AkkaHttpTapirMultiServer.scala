package perfTests

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import sttp.tapir._
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import akka.http.scaladsl.server.Directives.{concat}

import scala.io.StdIn
import scala.concurrent.Future

class AkkaHttpTapirMultiServer {
  implicit val actorSystem = ActorSystem(Behaviors.empty, "akka-http")
  implicit val executionContext = actorSystem.executionContext

  var bindingFuture: Option[scala.concurrent.Future[akka.http.scaladsl.Http.ServerBinding]] = None

  def setUp() = {
    def route(n: Int): Route = AkkaHttpServerInterpreter()
      .toRoute(
        endpoint
          .get
          .in("path" + n.toString)
          .in(path[Int]("id"))
          .errorOut(stringBody)
          .out(stringBody)
          .serverLogic((id: Int) =>
            Future.successful(Right((id + n).toString)): Future[Either[String, String]]
          )
      )

    this.bindingFuture = Some(
      Http()
        .newServerAt("127.0.0.1", 8080)
        .bind(
          Range(1,128,1)
            .foldLeft(route(0))(
              (acc, n) => concat(acc, route(n)))
        )
    )
  }

  def tearDown() = {
    bindingFuture match {
      case Some(b) => {
        b
          .flatMap(_.unbind())
          .onComplete(_ => actorSystem.terminate())
      }
      case None => ()
    }
  }
}

object AkkaHttpTapirMultiServer extends App {
  var server = new perfTests.AkkaHttpTapirMultiServer()
  server.setUp()
  println(s"Server now online. Please navigate to http://localhost:8080/path3/1\nPress RETURN to stop...")
  StdIn.readLine()
  server.tearDown()
  println(s"Server terminated")
}

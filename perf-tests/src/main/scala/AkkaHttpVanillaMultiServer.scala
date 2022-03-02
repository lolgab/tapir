
package perfTests

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn

class AkkaHttpVanillaMultiServer {
  implicit val actorSystem = ActorSystem(Behaviors.empty, "akka-http")
  implicit val executionContext = actorSystem.executionContext
  var bindingFuture: Option[scala.concurrent.Future[akka.http.scaladsl.Http.ServerBinding]] = None

  def setUp() = {
    val route = (n: Int) => get {
      path(("path" + n.toString) / IntNumber) {
        id => complete(id.toString)
      }
    }

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

object AkkaHttpVanillaMultiServer extends App {
  var server = new perfTests.AkkaHttpVanillaMultiServer()
  server.setUp()
  println(s"Server now online. Please navigate to http://localhost:8080/path0/1\nPress RETURN to stop...")
  StdIn.readLine()
  server.tearDown()
  println(s"Server terminated")
}

package typeracer

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Server extends App {

  implicit val system: ActorSystem = ActorSystem("typeracer")
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  val service: HttpRequest => Future[HttpResponse] =
    TypeRacerHandler(new TypeRacerImpl())

  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`application/json`, """{"status": "ok"}"""))
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8081)


  val binding = Http().bindAndHandleAsync(
    service,
    interface = "0.0.0.0",
    port = 8080
  )

  binding.onComplete {
    case Success(b) =>
      println(s"gRPC server bound to: ${b.localAddress}")
    case Failure(exception) =>
      system.log.error(exception, "Could not start server")
      system.terminate()
  }
  sys.addShutdownHook {
    system.terminate()
  }
}

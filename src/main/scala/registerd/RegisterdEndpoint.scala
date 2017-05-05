package registerd

import akka.pattern._
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.protobuf.ByteString
import registerd.RegisterdEndpoint.Protocol
import registerd.entity._
import registerd.entity.JsonProtocol._

import scala.util.{ Failure, Success, Try }
import scala.concurrent.duration._

case class RegisterdEndpoint(registerdRef: ActorRef)(implicit val system: ActorSystem) {
  implicit val valueFormat = jsonFormat2(Protocol.Value.apply)
  private val config = system.settings.config
  private val hostname = config.getString("registerd.hostname")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(10.seconds)
  implicit val printer = spray.json.PrettyPrinter
  val route = pathPrefix("v1") {
    pathPrefix("resources") {
      put {
        Directives.entity(as[Protocol.Value]) { request =>
          registerdRef ! Resource(
            instance = config.getString("registerd.hostname").asByteString,
            id = request.id.asByteString,
            payload = ByteString.copyFrom(request.payload.getBytes)
          )
          complete(StatusCodes.Accepted)
        }
      } ~
        get {
          path(Segment) { id =>
            onComplete(registerdRef.ask((hostname, id)).mapTo[Option[Resource]])(resourceOptRoute)
          } ~
            path(Segment / Segment) { (instance, id) =>
              onComplete(registerdRef.ask((instance, id)).mapTo[Option[Resource]])(resourceOptRoute)
            }
        }
    }
  }

  val bindHostname = system.settings.config.getString("registerd.endpoint.hostname")
  val bindPort = system.settings.config.getInt("registerd.endpoint.port")
  Http().bindAndHandle(route, bindHostname, bindPort)

  private def resourceOptRoute: PartialFunction[Try[Option[Resource]], Route] = {
    case Success(Some(resource)) =>
      complete(resource)
    case Success(None) =>
      complete(StatusCodes.NotFound)
    case Failure(t) =>
      system.log.error(t, t.getMessage)
      complete(StatusCodes.NotFound)
  }
}

object RegisterdEndpoint {

  object Protocol {
    case class Value(id: String, payload: String)
  }

}

package registerd

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.pattern._
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.cluster.http.management.ClusterHttpManagement
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.protobuf.ByteString
import com.typesafe.config.{ Config, ConfigFactory }
import registerd.entity.Resource

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object Registerd {

  val config: Config = ConfigFactory.load

  private val roles = config.getStringList("akka.cluster.roles").toList

  def main(args: Array[String]): Unit = {
    val cluster = Cluster(ActorSystem("registerd", config))

    onRole("seed") {
      ClusterHttpManagement(cluster)
    }
    onRole("registerd") {
      startRegisterd(cluster)
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn

  private def hasRole(role: String): Boolean = roles.contains(role)

  private def startRegisterd(cluster: Cluster): Unit = {
    implicit val system = cluster.system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    implicit val timeout = Timeout(10.seconds)
    val ref = system.actorOf(Props(classOf[Registerd], cluster), config.getString("registerd.hostname"))
    import registerd.entity.JsonProtocol._
    implicit val format = jsonFormat2(Protocol.Value.apply)
    implicit val printer = spray.json.PrettyPrinter
    val route =
      pathPrefix("resources") {
        put {
          Directives.entity(as[Protocol.Value]) { request =>
            ref ! Resource(
              instance = config.getString("registerd.hostname"),
              instanceUri = config.getString("registerd.hostname") + s"/${request.id}",
              payload = ByteString.copyFrom(request.payload.getBytes)
            )
            complete(StatusCodes.Accepted)
          }
        } ~
          get {
            path(Segment) { id =>
              onComplete(ref.ask(id).mapTo[Option[Resource]]) {
                case Success(Some(resource)) =>
                  complete(resource)
                case Success(None) =>
                  complete(StatusCodes.NotFound)
                case Failure(t) =>
                  system.log.error(t, t.getMessage)
                  complete(StatusCodes.NotFound)
              }
            }
          }
      }
    val hostname = system.settings.config.getString("registerd.endpoint.hostname")
    val port = system.settings.config.getInt("registerd.endpoint.port")
    Http().bindAndHandle(route, hostname, port)
  }
  object Protocol {
    case class Value(id: String, payload: String)
  }
}

class Registerd(cluster: Cluster) extends Actor with ActorLogging {
  def receive = {
    case message => log.info(s"message received: $message")
  }

  override def unhandled(message: Any): Unit = log.warning(s"unhandled message: $message")
}

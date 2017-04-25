package registerd

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import registerd.entity.{ Block, Payload }

class RegisterdInstance extends Actor with ActorLogging {
  log.info(s"instantiate: ${self.path}")
  def receive = {
    case message => log.info(s"message received: $message")
  }
}

object RegisterdInstance {
  def apply(cluster: Cluster): Unit = {
    implicit val system = cluster.system
    val config = system.settings.config
    system.actorOf(Props(classOf[RegisterdInstance]), config.getString("registerd.hostname"))
    startServer(system)
  }
  private def startServer(implicit system: ActorSystem): Unit = {
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    import registerd.entity.JsonProtocol._
    implicit val printer = spray.json.PrettyPrinter
    val route =
      path("resources") {
        put {
          complete("")
        } ~ get {
          complete(Block.defaultInstance.copy(payloads = List(Payload.defaultInstance)))
        }
      }
    val hostname = system.settings.config.getString("registerd.endpoint.hostname")
    val port = system.settings.config.getInt("registerd.endpoint.port")
    Http().bindAndHandle(route, hostname, port)
  }
}

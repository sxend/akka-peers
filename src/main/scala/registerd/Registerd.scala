package registerd

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.cluster.http.management.ClusterHttpManagement
import akka.stream.ActorMaterializer
import com.typesafe.config.{ Config, ConfigFactory }
import registerd.entity.{ Block, Payload }

import scala.collection.JavaConversions._

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
    system.actorOf(Props(classOf[Registerd], cluster), config.getString("registerd.hostname"))
  }
}

class Registerd(cluster: Cluster) extends Actor with ActorLogging {
  def receive = {
    case message => log.info(s"message received: $message")
  }

  override def unhandled(message: Any): Unit = log.warning(s"unhandled message: $message")
}
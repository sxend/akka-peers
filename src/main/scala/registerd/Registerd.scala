package registerd

import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConversions._
import scala.concurrent.duration._

object Registerd {

  val config: Config = ConfigFactory.load

  private val roles = config.getStringList("akka.cluster.roles").toList

  private implicit val timeout = Timeout(120.seconds)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("registerd", config)
    val cluster = Cluster(system)
    onRole("seed") {
      CounterActor.startProxy(system)
      ClusterHttpManagement(cluster).start()
    }
    onRole("member") {
      CounterActor.startSharding(system)
      startServer()
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn

  private def hasRole(role: String): Boolean = roles.contains(role)

  private def startServer()(implicit system: ActorSystem): Unit = {
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    val route =
      path("blocks" / Segment) { (id) =>
        get {
          complete("endpoint available")
        }
      }
    Http().bindAndHandle(route, "localhost", 8080)
  }
}
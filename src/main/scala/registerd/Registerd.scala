package registerd

import java.util.UUID

import akka.actor.ActorRef
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.io.Source

object Registerd {

  lazy val config = ConfigFactory.load
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
      path("") {
        get {
          complete("endpoint available")
        }
      }
    Http().bindAndHandle(route, "localhost", 8080)
  }
}
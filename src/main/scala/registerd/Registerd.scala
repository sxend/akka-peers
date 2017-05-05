package registerd

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.pattern._
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Subscribe, SubscribeAck }
import com.typesafe.config.{ Config, ConfigFactory }
import registerd.entity._

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object Registerd {

  val config: Config = ConfigFactory.load
  private val hostname = config.getString("registerd.hostname")
  private val roles: List[String] = config.getStringList("akka.cluster.roles").asScala.toList

  def main(args: Array[String]): Unit = {
    val cluster = Cluster(ActorSystem("registerd", config))

    onRole("seed") {
      ClusterHttpManagement(cluster)
    }
    onRole("registerd") {
      RegisterdEndpoint(registerdRef(cluster), cluster)
    }
  }
  private def onRole(role: String)(fn: => Unit) = if (hasRole(role)) fn

  private def hasRole(role: String): Boolean = roles.contains(role)

  private def registerdRef(cluster: Cluster) =
    cluster.system.actorOf(Props(classOf[Registerd], cluster), hostname)
}

class Registerd(cluster: Cluster) extends Actor with ActorLogging {

  import context.dispatcher
  private implicit val system = context.system
  private val mediator = DistributedPubSub(system).mediator
  private val settings = system.settings
  private val config = settings.config
  private val hostname = config.getString("registerd.hostname")
  private val resourcesDir = config.getString("registerd.resources-dir")

  mediator ! Subscribe("resource", self)

  def receive = {
    case (instance: String, id: String) =>
      if (instance == hostname)
        getResource(instance, id).pipeTo(sender())
    case resource: Resource => saveResource(resource)
  }

  private def getResource(instance: String, id: String): Future[Option[Resource]] = {
    Future {
      val checksum = FileSystem.readString(checksumFile(instance, id))
      val resource = Resource.parseFrom(FileSystem.readBinary(resourceFile(instance, id)))
      val resourceDigest = resource.digest
      if (checksum == resourceDigest) {
        Some(resource)
      } else {
        log.warning(s"$resourcesDir/$instance/$id checksum mismatch: $checksum != $resourceDigest")
        None
      }
    }
  }

  private def saveResource(resource: Resource): Unit = {
    val instance = resource.instance.asString
    val id = resource.id.asString
    FileSystem.writeString(checksumFile(instance, id), resource.digest)
    FileSystem.writeBinary(resourceFile(instance, id), resource.toByteArray)
  }

  private def checksumFile(instance: String, id: String): String =
    s"$resourcesDir/$instance/$id/checksum.txt"
  private def resourceFile(instance: String, id: String): String =
    s"$resourcesDir/$instance/$id/resource.bin"

  override def unhandled(message: Any): Unit = message match {
    case _: SubscribeAck =>
    case _               => log.warning(s"unhandled message: $message")
  }
}

package registerd

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern._
import akka.cluster.Cluster
import akka.cluster.http.management.ClusterHttpManagement
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.typesafe.config.{Config, ConfigFactory}
import registerd.entity._

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object Registerd {

  val config: Config = ConfigFactory.load
  private val hostname = config.getString("registerd.hostname")
  private val roles: List[String] = config.getStringList("akka.cluster.roles").asScala.toList

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("example", config)
    val mediator = Peers(system).mediator
    mediator ! PeersMediator.Join(ref)
  }

  private def registerdRef(system: ActorSystem) =
    system.actorOf(Props(classOf[Registerd]), hostname)
}

class Registerd extends Actor with ActorLogging {

  import context.dispatcher
  private implicit val system = context.system
  private val settings = system.settings
  private val config = settings.config
  private val hostname = config.getString("registerd.hostname")
  private val resourcesDir = config.getString("registerd.resources-dir")

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
      val hash = resource.hash
      if (checksum == hash) {
        Some(resource)
      } else {
        log.warning(s"$resourcesDir/$instance/$id checksum mismatch: $checksum != $hash")
        None
      }
    }
  }

  private def saveResource(resource: Resource): Unit = {
    val instance = resource.instance.asString
    val id = resource.id.asString
    FileSystem.writeString(checksumFile(instance, id), resource.hash)
    FileSystem.writeBinary(resourceFile(instance, id), resource.toByteArray)
  }

  private def checksumFile(instance: String, id: String): String =
    s"$resourcesDir/$instance/$id/checksum.txt"
  private def resourceFile(instance: String, id: String): String =
    s"$resourcesDir/$instance/$id/resource.bin"

  override def unhandled(message: Any): Unit = message match {
    case _ => log.warning(s"unhandled message: $message")
  }
}

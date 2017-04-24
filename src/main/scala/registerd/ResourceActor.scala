package registerd

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.cluster.sharding.ShardRegion.{ HashCodeMessageExtractor, MessageExtractor }
import org.apache.commons.codec.digest.DigestUtils
import registerd.entity.Payload

class ResourceActor extends Actor with ActorLogging {
  var state: Payload = Payload.defaultInstance
  def receive = {
    case resource: Array[Byte] =>
      val opt = if (this.state.resource.isEmpty) None else Option(this.state)
      sender() ! opt
    case payload: Payload =>
      this.state = payload
  }
}

object ResourceActor {
  val typeName: String = "resource"
  val messageExtractor: MessageExtractor = new HashCodeMessageExtractor(100) {
    override def entityId(message: Any): String = message match {
      case message: Array[Byte] => DigestUtils.sha256Hex(message)
      case payload: Payload     => DigestUtils.sha256Hex(payload.resource.toByteArray)
    }
  }
  def startProxy(system: ActorSystem, role: String = "member"): ActorRef =
    ClusterSharding(system).startProxy(
      typeName = typeName,
      role = java.util.Optional.of(role),
      messageExtractor = messageExtractor
    )
  def startSharding(system: ActorSystem, role: String = "member"): ActorRef =
    ClusterSharding(system).start(
      typeName = typeName,
      entityProps = Props(classOf[ResourceActor]),
      settings = ClusterShardingSettings(system).withRole(role),
      messageExtractor = messageExtractor
    )
}

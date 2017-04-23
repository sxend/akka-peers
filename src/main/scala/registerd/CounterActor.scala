package registerd

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.cluster.sharding.ShardRegion.{ HashCodeMessageExtractor, MessageExtractor }

class CounterActor extends Actor with ActorLogging {
  private val counter = new AtomicLong(0)
  def receive = {
    case msg: String =>
      sender() ! (self.path.toString, counter.getAndIncrement().toString, CounterActor.padding)
  }
}

object CounterActor {
  lazy val padding: String = (0 until 1000000).map(_ => "0").mkString
  val typeName: String = "counter"
  val messageExtractor: MessageExtractor = new HashCodeMessageExtractor(10) {
    override def entityId(message: Any): String = message match {
      case message: String => (message.take(10).hashCode % 10).toString
    }
  }
  def startProxy(system: ActorSystem, role: String = "node"): ActorRef =
    ClusterSharding(system).startProxy(
      typeName = typeName,
      role = java.util.Optional.of(role),
      messageExtractor = messageExtractor
    )
  def startSharding(system: ActorSystem, role: String = "node"): ActorRef =
    ClusterSharding(system).start(
      typeName = typeName,
      entityProps = Props(classOf[CounterActor]),
      settings = ClusterShardingSettings(system).withRole(role),
      messageExtractor = messageExtractor
    )
}

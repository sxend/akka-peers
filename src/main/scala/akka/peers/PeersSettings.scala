package akka.peers

import java.time.Duration

import akka.actor.{ Address, AddressFromURIString }
import akka.dispatch.Dispatchers
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.collection.JavaConverters._

class PeersSettings(val config: Config, val systemName: String) {
  private val cc = config.getConfig("akka.peers")
  val name: String = cc.getString("name")
  val useDispatcher: String = cc.getString("use-dispatcher") match {
    case "" => Dispatchers.DefaultDispatcherId
    case id => id
  }
  val heartbeatInterval: FiniteDuration = cc.getDuration("heartbeat-interval").toMillis.millis
  val heartbeatTimeout: FiniteDuration = cc.getDuration("heartbeat-timeout").toMillis.millis
  val exploringThreshold: Int = cc.getInt("exploring-threshold")
  val seedPeers: List[Address] =
    cc.getStringList("seed-peers").asScala.toList.map(AddressFromURIString.apply)
  val seedResolvers: List[Address] =
    cc.getStringList("seed-resolvers").asScala.toList.map(AddressFromURIString.apply)
  require(seedPeers.nonEmpty || seedResolvers.nonEmpty, "seed-peers is nonEmpty or seed-resolvers is nonEmpty")
  val resolveResultLimit: Int = cc.getInt("resolve-result-limit")
}

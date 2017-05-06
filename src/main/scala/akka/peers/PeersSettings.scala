package akka.peers

import akka.dispatch.Dispatchers
import com.typesafe.config.Config

class PeersSettings(val config: Config, val systemName: String) {
  private val cc = config.getConfig("akka.peers")
  val name: String = cc.getString("name")
  val useDispatcher: String = cc.getString("use-dispatcher") match {
    case "" => Dispatchers.DefaultDispatcherId
    case id => id
  }
}

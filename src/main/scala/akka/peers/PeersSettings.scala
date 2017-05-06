package akka.peers

import com.typesafe.config.Config

class PeersSettings(val config: Config, val systemName: String) {
  private val cc = config.getConfig("akka.peers")
}

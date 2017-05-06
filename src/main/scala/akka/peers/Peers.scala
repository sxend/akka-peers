package akka.peers

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.dispatch.Dispatchers

object Peers extends ExtensionId[Peers] with ExtensionIdProvider {
  override def lookup = Peers
  override def createExtension(system: ExtendedActorSystem): Peers = new Peers(system)

}
class Peers(val system: ExtendedActorSystem) extends Extension {
  val settings = new PeersSettings(system.settings.config, system.name)
  val coordinator: ActorRef = {
    val name = system.settings.config.getString("akka.peers.name")
    val dispatcher = system.settings.config.getString("akka.peers.use-dispatcher") match {
      case "" => Dispatchers.DefaultDispatcherId
      case id => id
    }
    system.systemActorOf(PeersCoordinator.props(settings).withDispatcher(dispatcher), name)
  }
}
package akka.peers

import akka.actor.{ ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }

object Peers extends ExtensionId[Peers] with ExtensionIdProvider {
  override def lookup = Peers
  override def createExtension(system: ExtendedActorSystem): Peers = new Peers(system)

}
class Peers(val system: ExtendedActorSystem) extends Extension {
  val settings = new PeersSettings(system.settings.config, system.name)
  private val props = PeersCoordinator.props(settings).withDispatcher(settings.useDispatcher)
  val coordinator: ActorRef = system.systemActorOf(props, settings.name)
}
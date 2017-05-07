package akka.peers

import akka.actor.{ ActorPath, ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import scala.concurrent.duration._

object Peers extends ExtensionId[Peers] with ExtensionIdProvider {
  override def lookup = Peers
  override def createExtension(system: ExtendedActorSystem): Peers = new Peers(system)
}
class Peers(val system: ExtendedActorSystem) extends Extension {
  val settings = new PeersSettings(system.settings.config, system.name)
  private[peers] var _neighbors: Set[ActorPath] = Set.empty
  def neighbors: Set[ActorPath] = _neighbors
  system.systemActorOf(PeersCoordinator.props(settings).withDispatcher(settings.useDispatcher), settings.name)
}
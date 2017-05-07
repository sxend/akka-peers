package akka.peers

import akka.actor.{ ActorPath, ActorRef, Address, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import akka.remote.RemoteActorRefProvider

import scala.concurrent.duration._

object Peers extends ExtensionId[Peers] with ExtensionIdProvider {
  override def lookup = Peers
  override def createExtension(system: ExtendedActorSystem): Peers = new Peers(system)
}
class Peers(val system: ExtendedActorSystem) extends Extension {
  val address = system.provider match {
    case rarp: RemoteActorRefProvider => rarp.transport.defaultAddress
    case _                            => system.provider.rootPath.address
  }
  val settings = new PeersSettings(system.settings.config, system.name)
  private[peers] var _neighbors: Set[Address] = Set.empty
  private[peers] def addNeighbors(address: Address): Unit =
    this._neighbors = this._neighbors + address
  private[peers] def removeNeighbors(address: Address): Unit = {
    system.log.info(s"this._neighbors: ${this._neighbors}, address: $address, this._neighbors.filterNot(_ == address): ${this._neighbors.filterNot(_ == address)}")
    this._neighbors = this._neighbors.filterNot(_ == address)
    system.log.info(s"this._neighbors: ${this._neighbors}")
  }

  def neighbors: Set[Address] = _neighbors

  system.systemActorOf(PeersCoordinator.props(settings).withDispatcher(settings.useDispatcher), settings.name)
}
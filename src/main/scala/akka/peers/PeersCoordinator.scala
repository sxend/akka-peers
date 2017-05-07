package akka.peers

import akka.pattern._
import akka.actor.{ Actor, ActorLogging, ActorSelection, Address, AddressFromURIString, Props }
import akka.peers.protobuf._
import akka.peers.util.Syntax._
import akka.util.Timeout

import scala.util.{ Failure, Success }
import scala.util.Random.shuffle
import scala.concurrent.duration._

object PeersCoordinator {
  def props(settings: PeersSettings) = Props(classOf[PeersCoordinator], settings)
  object Protocol {
    case class RemoveNeighbor(address: Address)
  }
}

class PeersCoordinator(settings: PeersSettings) extends Actor with ActorLogging {
  import context.dispatcher
  import PeersCoordinator.Protocol
  private val peers = Peers(context.system)
  def receive: Receive = {
    case handshake: Handshake =>
      val address = handshake.address.parsedAddress
      log.debug(s"incoming handshake: $handshake")
      sender() ! HandshakeAck(selfAddress.toString)
      if (!peers.neighbors.contains(address)) {
        sender() ! Handshake(selfAddress.toString)
      }
    case handshakeAck: HandshakeAck =>
      log.debug(s"incoming handshakeAck: $handshakeAck")
      peers.addNeighbors(handshakeAck.address.parsedAddress)
    case heartbeat: Heartbeat if peers.neighbors.contains(heartbeat.address.parsedAddress) =>
      log.debug(s"incoming heartbeat: $heartbeat")
      sender() ! HeartbeatAck(selfAddress.toString)
    case query: PeerResolveQuery =>
      log.debug(s"incoming query: $query")
      val limit = Option(query.limit).filter(_ > 0).getOrElse(settings.resolveResultLimit)
      val result = shuffle(peers.neighbors.filterNot(_ == query.address.parsedAddress))
        .take(math.min(limit, settings.resolveResultLimit))
        .map(_.toString).toList
        .wrap(PeerResolveResult.apply)
      sender() ! result
    case result: PeerResolveResult =>
      log.debug(s"incoming result: $result")
      result.addresses.foreach { address =>
        address.parsedAddress.coordinatorRef ! Handshake(selfAddress.toString)
      }
    case Protocol.RemoveNeighbor(address) =>
      log.debug(s"incoming RemoveNeighbor: $address")
      peers.removeNeighbors(address)
  }

  private val selfAddress: Address = peers.address

  private def heartbeat(): Unit = {
    implicit val timeout: Timeout = settings.heartbeatTimeout
    if (peers.neighbors.size < settings.exploringThreshold) {
      startup()
    } else {
      peers.neighbors.foreach { address =>
        address.coordinatorRef.ask(Heartbeat(selfAddress.toString)).mapTo[HeartbeatAck].onComplete {
          case Success(ack) => log.debug(s"heartbeat succeed: $address. ack from: ${ack.address}")
          case Failure(t) =>
            log.error(t, s"heartbeat failed: ${t.getMessage}")
            self ! Protocol.RemoveNeighbor(address)
        }
      }
    }
  }
  context.system.scheduler.schedule(0.seconds, settings.heartbeatInterval)(heartbeat())
  private def startup(): Unit = {
    settings.seedPeers.filterNot(_ == selfAddress).foreach { address =>
      address.coordinatorRef ! Handshake(selfAddress.toString)
    }
    settings.seedResolvers.filterNot(_ == selfAddress).foreach { address =>
      address.coordinatorRef !
        PeerResolveQuery(selfAddress.toString, settings.resolveResultLimit)
    }
  }

  private implicit class ToCoordinatorPath(address: Address) {
    def coordinatorPath: String = s"${address.toString}/system/${settings.name}"
    def coordinatorRef: ActorSelection =
      context.system.actorSelection(address.coordinatorPath)
  }
  private implicit class ParseAddress(addressStr: String) {
    def parsedAddress: Address = AddressFromURIString(addressStr)
  }
}
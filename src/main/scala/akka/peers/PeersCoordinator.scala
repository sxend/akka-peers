package akka.peers

import akka.pattern._
import akka.actor.{ Actor, ActorLogging, ActorRef, Address, AddressFromURIString, Props }
import akka.peers.protobuf._
import akka.remote.RemoteActorRefProvider
import akka.remote.transport.TransportAdaptersExtension
import akka.util.Timeout

import scala.util.{ Failure, Success }
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
      log.info(s"incomming handshake: $handshake")
      sender() ! HandshakeAck(selfAddress)
      if (!peers.neighbors.contains(AddressFromURIString(handshake.address))) {
        sender() ! Handshake(selfAddress)
      }
    case handshakeAck: HandshakeAck =>
      log.info(s"incomming handshakeAck: $handshakeAck")
      peers.addNeighbors(AddressFromURIString(handshakeAck.address))
    case heartbeat: Heartbeat if peers.neighbors.contains(AddressFromURIString(heartbeat.address)) =>
      log.info(s"incomming heartbeat: $heartbeat")
      sender() ! Heartbeat(selfAddress)
    case query: PeerResolveQuery =>
      log.info(s"incomming query: $query")
      val limit = Option(query.limit).filter(_ > 0).getOrElse(settings.resolveResultLimit)
      val result = {
        val list = peers.neighbors.filterNot(_.toString == query.address)
          .take(math.min(limit, settings.resolveResultLimit))
          .map(address => address.toString).toList
        PeerResolveResult(list)
      }
      sender() ! result
    case result: PeerResolveResult =>
      log.info(s"incomming result: $result")
      result.addresses.foreach { address =>
        context.system.actorSelection(s"$address/system/${settings.name}") ! Handshake(selfAddress)
      }
    case Protocol.RemoveNeighbor(address) =>
      log.info(s"incomming RemoveNeighbor: $address")
      peers.removeNeighbors(address)
  }

  private val selfAddress = peers.address.toString

  private def heartbeat(): Unit = {
    implicit val timeout: Timeout = settings.heartbeatTimeout
    if (peers.neighbors.size < settings.exploringThreshold) {
      startup()
    } else {
      peers.neighbors.foreach { address =>
        val selection = s"$address/system/${settings.name}"
        context.system.actorSelection(selection).ask(Heartbeat(selfAddress)).mapTo[Heartbeat].onComplete {
          case Success(s) => log.debug(s"heartbeat succeed: {}.", address)
          case Failure(t) =>
            log.warning(s"heartbeat failed: {}. {} {}", address, t.getMessage, t.getStackTrace)
            self ! Protocol.RemoveNeighbor(address)
        }
      }
    }
  }
  context.system.scheduler.schedule(0.seconds, settings.heartbeatInterval)(heartbeat())
  private def startup(): Unit = {
    log.info(s"selfaddress: $selfAddress")
    settings.seedPeers.filterNot(_ == selfAddress).foreach { address =>
      val selection = s"$address/system/${settings.name}"
      log.info(s"select address: $selection")
      context.system.actorSelection(selection) ! Handshake(selfAddress)
    }
    settings.seedResolvers.filterNot(_ == selfAddress).foreach { address =>
      val selection = s"$address/system/${settings.name}"
      log.info(s"select address: $selection")
      context.system.actorSelection(selection) ! PeerResolveQuery(selfAddress, settings.resolveResultLimit)
    }
  }
}
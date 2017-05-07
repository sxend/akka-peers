package akka.peers

import akka.pattern._
import akka.actor.{ Actor, ActorLogging, ActorPath, ActorRef, Props }
import akka.peers.protobuf._
import akka.util.Timeout

import scala.util.{ Failure, Success }

object PeersCoordinator {
  def props(settings: PeersSettings) = Props(classOf[PeersCoordinator], settings)
  object Protocol {
    case class RemoveNeighbor(path: ActorPath)
  }
}

class PeersCoordinator(settings: PeersSettings) extends Actor with ActorLogging {
  import context.dispatcher
  import PeersCoordinator.Protocol
  private val peers = Peers(context.system)
  def receive: Receive = {
    case handshake: Handshake =>
      peers._neighbors = peers.neighbors + ActorPath.fromString(handshake.path)
      sender() ! HandshakeAck(self.path.root.toStringWithAddress(self.path.address))
    case handshakeAck: HandshakeAck =>
      peers._neighbors = peers.neighbors + ActorPath.fromString(handshakeAck.path)
    case heartbeat: Heartbeat if peers.neighbors.contains(ActorPath.fromString(heartbeat.path)) =>
      sender() ! Heartbeat(selfPath)
    case Protocol.RemoveNeighbor(path) =>
      peers._neighbors = peers._neighbors.filterNot(_ == path)
  }
  private def selfPath =
    self.path.root.toStringWithAddress(self.path.address)

  private def heartbeat() = {
    implicit val timeout: Timeout = settings.heartbeatTimeout
    peers.neighbors.foreach { path =>
      context.system.actorSelection(path).ask(Heartbeat(selfPath)).mapTo[Heartbeat].onComplete {
        case Success(s) => log.debug(s"heartbeat succeed: {}.", path)
        case Failure(t) =>
          log.warning(s"heartbeat failed: {}. {} {}", path, t.getMessage, t.getStackTrace)
          self ! Protocol.RemoveNeighbor(path)
      }
    }
  }
}
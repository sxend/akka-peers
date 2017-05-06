package akka.peers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object PeersCoordinator {
  case class Subscribe(topic: String, ref: ActorRef)
  case class Publish(topic: String, message: Any)
  def props(settings: PeersSettings) = Props(classOf[PeersCoordinator], settings)
}

class PeersCoordinator(settings: PeersSettings) extends Actor with ActorLogging {
  var listeners: Map[String, ActorRef] = Map.empty
  def receive: Receive = {
    case PeersCoordinator.Subscribe(topic, ref) =>
      this.listeners = listeners ++ Map(topic -> ref)
    case PeersCoordinator.Publish(topic, message) =>
      this.listeners.filter(_ == topic).foreach {
        case (_, ref) => ref ! message
      }
  }
}
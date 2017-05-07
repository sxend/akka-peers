package example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.peers.{Peers, PeersCoordinator}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("peers-example")
    system.actorOf(Props(classOf[Example]))
  }
}

class Example extends Actor with ActorLogging {
  import scala.concurrent.duration._
  private val peers = Peers(context.system)
  private val coordinator = peers.heartbeater
  coordinator ! PeersCoordinator.Subscribe("heartbeat", self)
  context.system.scheduler.schedule(1.seconds, 30.seconds) {
    coordinator ! PeersCoordinator.Publish("heartbeat", s"hello. I am ${self.path}")
  }
  def receive: Receive = {
    case message => log.info(s"receive message: $message")
  }
}
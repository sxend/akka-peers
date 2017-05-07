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
  import context.dispatcher
  import scala.concurrent.duration._
  private val peers = Peers(context.system)
  context.system.scheduler.schedule(1.seconds, 10.seconds) {
    self ! peers.neighbors
  }
  def receive: Receive = {
    case message => log.info(s"receive message: $message")
  }
}
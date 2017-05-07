package example

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.peers.Peers

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("example")
    system.actorOf(Props(classOf[Example]), "example-actor")
  }
}

class Example extends Actor with ActorLogging {
  import context.dispatcher
  import scala.concurrent.duration._
  private val peers = Peers(context.system)
  context.system.scheduler.schedule(1.seconds, 10.seconds) {
    peers.neighbors.foreach { address =>
      val selection = s"${address.toString}/user/example-actor"
      context.actorSelection(selection) ! s"send from $actorPath"
    }
  }
  def receive: Receive = {
    case message =>
      log.info(s"$actorPath receive message: $message")
  }
  private def actorPath = s"${peers.address}${self.path.toStringWithoutAddress}"
}
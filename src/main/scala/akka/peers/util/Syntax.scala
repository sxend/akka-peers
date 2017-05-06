package akka.peers.util

private[peers] trait Syntax {
  implicit class Tapper[A](self: A) {
    def tap(fn: A => Unit): A = {
      fn(self)
      self
    }
  }
}

private[peers] object Syntax extends Syntax
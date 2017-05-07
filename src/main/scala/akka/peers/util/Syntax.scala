package akka.peers.util

private[peers] trait Syntax {
  private[peers] implicit class Tapper[A](self: A) {
    def tap(fn: A => Unit): A = {
      fn(self)
      self
    }
  }
  private[peers] implicit class Wrapper[A](self: A) {
    def wrap[B](fn: A => B): B = fn(self)
  }
}

private[peers] object Syntax extends Syntax
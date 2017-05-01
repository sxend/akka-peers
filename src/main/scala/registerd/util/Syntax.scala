package registerd.util

trait Syntax {
  implicit class Tapper[A](self: A) {
    def tap(fn: A => Unit): A = {
      fn(self)
      self
    }
  }
}

object Syntax extends Syntax
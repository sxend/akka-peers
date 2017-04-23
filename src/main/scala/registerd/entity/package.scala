package registerd

import org.apache.commons.codec.digest.DigestUtils

package object entity {

  implicit class BlockOps(block: Block) {
    lazy val digest: String = hash(block.toByteArray)
  }
  implicit class PayloadOps(payload: Payload) {
    lazy val digest: String = hash(payload.toByteArray)
  }
  implicit class PayloadsOps(payloads: List[Payload]) {
    lazy val digest: String = flatten(payloads.map(_.digest): _*)

    private def flatten(digests: String*): String = digests match {
      case a :: Nil       => hash(a + a)
      case a :: b :: Nil  => hash(a + b)
      case a :: b :: tail => hash(flatten(a, b) + flatten(tail: _*))
    }
  }

  private def hash(data: Array[Byte]): String = DigestUtils.sha256Hex(data)
  private def hash(data: String): String = DigestUtils.sha256Hex(data)

}

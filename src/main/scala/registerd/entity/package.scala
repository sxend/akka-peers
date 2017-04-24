package registerd

import org.apache.commons.codec.digest.DigestUtils

package object entity {

  implicit class BlockOps(block: Block) {
    lazy val digest: Array[Byte] = hash(block.toByteArray)
  }
  implicit class PayloadOps(payload: Payload) {
    lazy val digest: Array[Byte] = hash(payload.toByteArray)
  }
  implicit class PayloadsOps(payloads: List[Payload]) {
    lazy val digest: Array[Byte] = flatten(payloads.map(_.digest): _*)

    private def flatten(digests: Array[Byte]*): Array[Byte] = digests match {
      case a :: Nil       => hash(a ++ a)
      case a :: b :: Nil  => hash(a ++ b)
      case a :: b :: tail => hash(flatten(a, b) ++ flatten(tail: _*))
    }
  }

  private def hash(data: Array[Byte]): Array[Byte] = DigestUtils.sha256(data)

}

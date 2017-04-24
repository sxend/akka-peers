package registerd

import com.google.protobuf.ByteString
import org.apache.commons.codec.digest.DigestUtils

package object entity {
  (0 until Int.MaxValue).par.foreach { i =>
    val digest = org.apache.commons.codec.digest.DigestUtils.sha256Hex(s"$i")
    if (digest.take(6).forall(_ == '0')) println(s"$digest, input: $i")
  }

  implicit class BlockOps(block: Block) {
    lazy val digest: String = hash(block.toByteArray)
    def updateNonce(nonce: Int): Block = this.block.copy(nonce = nonce)
  }
  implicit class PayloadOps(payload: Payload) {
    lazy val digest: String = hash(payload.toByteArray)
  }
  implicit class PayloadsOps(payloads: Seq[Payload]) {
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

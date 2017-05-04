package registerd

import com.google.protobuf.ByteString
import org.apache.commons.codec.digest.DigestUtils

package object entity {

  implicit class BlockOps(block: Block) {
    lazy val digest: String = hash(block.toByteArray)
    def updateNonce(nonce: Int): Block = this.block.copy(nonce = nonce)
  }
  implicit class ResourceOps(resource: Resource) {
    lazy val digest: String = hash(resource.toByteArray)
  }
  implicit class ResourcesOps(resources: Seq[Resource]) {
    lazy val digest: String = flatten(resources.map(_.digest): _*)

    private def flatten(digests: String*): String = digests match {
      case a :: Nil       => hash(a + a)
      case a :: b :: Nil  => hash(a + b)
      case a :: b :: tail => hash(flatten(a, b) + flatten(tail: _*))
    }
  }
  implicit class StringToByteString(str: String) {
    def asByteString: ByteString = ByteString.copyFrom(str.getBytes("UTF-8"))
  }
  implicit class ByteStringToString(bs: ByteString) {
    def asString: String = new String(bs.toByteArray, "UTF-8")
  }

  private def hash(data: Array[Byte]): String = DigestUtils.sha256Hex(data)
  private def hash(data: String): String = DigestUtils.sha256Hex(data)

}

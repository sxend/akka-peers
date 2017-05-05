package registerd

import com.google.protobuf.ByteString
import org.apache.commons.codec.digest.DigestUtils

package object entity {

  implicit class BlockOps(block: Block) {
    lazy val hash: String = digest(block.toByteArray)
    def updateNonce(nonce: Int): Block = this.block.copy(nonce = nonce)
  }
  implicit class ResourceOps(resource: Resource) {
    lazy val hash: String = digest(resource.toByteArray)
  }
  implicit class ResourcesOps(resources: Seq[Resource]) {
    lazy val hash: String = flatten(resources.map(_.hash): _*)

    private def flatten(hashes: String*): String = hashes match {
      case a :: Nil       => digest(a + a)
      case a :: b :: Nil  => digest(a + b)
      case a :: b :: tail => digest(flatten(a, b) + flatten(tail: _*))
    }
  }
  implicit class StringToByteString(str: String) {
    def asByteString: ByteString = ByteString.copyFrom(str.getBytes("UTF-8"))
  }
  implicit class ByteStringToString(bs: ByteString) {
    def asString: String = new String(bs.toByteArray, "UTF-8")
  }

  private def digest(data: Array[Byte]): String = DigestUtils.sha256Hex(data)
  private def digest(data: String): String = DigestUtils.sha256Hex(data)

}

package registerd.entity

import com.google.protobuf.ByteString
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.{ JsValue, RootJsonFormat }

trait JsonProtocol {
  implicit val byteStringFormat = new RootJsonFormat[ByteString] {
    override def read(json: JsValue): ByteString = json match {
      case JsArray(arr) => ByteString.copyFrom(arr.map(_.convertTo[Byte]).toArray)
      case obj          => throw new RuntimeException(s"Expected JsArray, but got $obj")
    }
    override def write(obj: ByteString): JsValue = {
      JsArray(obj.toByteArray.map(_.toJson): _*)
    }
  }
  implicit val payloadFormat = jsonFormat4(Payload.apply)
  implicit val blockFormat = jsonFormat6(Block.apply)
}

object JsonProtocol extends JsonProtocol

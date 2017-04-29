package registerd.entity

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.google.protobuf.ByteString
import spray.json._

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val byteStringFormat = new RootJsonFormat[ByteString] {
    override def read(json: JsValue): ByteString = json match {
      case JsArray(arr) => ByteString.copyFrom(arr.map(_.convertTo[Byte]).toArray)
      case obj          => throw new RuntimeException(s"Expected JsArray, but got $obj")
    }
    override def write(obj: ByteString): JsValue = {
      JsArray(obj.toByteArray.map(_.toJson): _*)
    }
  }
  implicit val payloadFormat = jsonFormat4(Resource.apply)
  implicit val blockFormat = jsonFormat6(Block.apply)
}

object JsonProtocol extends JsonProtocol

package controllers

import play.api.http.ContentTypeOf
import com.google.protobuf.Message
import play.api.http.Writeable

object Implicits {
  implicit def contentTypeOf_Protobuf: ContentTypeOf[Message] = {
    ContentTypeOf[Message](Some("application/x-protobuf"))
  }
  implicit def writeableOf_Protobuf: Writeable[Message] = {
    Writeable[Message](message => message.toByteArray())
  }
}

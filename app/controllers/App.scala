package controllers

import messages.Main._
import play.api.mvc._
import play.api.http.{ Writeable, ContentTypeOf }
import com.google.protobuf.Message
import play.api.libs.iteratee.Iteratee

object Application extends App {
  val accounts = collection.mutable.Map[String,Account]()
}

trait App extends Controller {
  
  val accounts: collection.mutable.Map[String,Account]

  implicit def contentTypeOf_Protobuf: ContentTypeOf[Message] = {
    ContentTypeOf[Message](Some("application/x-protobuf"))
  }
  implicit def writeableOf_Protobuf: Writeable[Message] = {
    Writeable[Message](message => message.toByteArray())
  }

  def accountGet(id: String) = Action { request =>
    request.headers.get("Authorization").map { auth =>
      if (auth == "Bob") {
        val account = accounts(id)
        Ok(account)
      } else {
        Unauthorized
      }
    } getOrElse Unauthorized
  }

  def accountPut(id: String) = Action { request =>
    request.body.asRaw.flatMap { raw =>
      raw.asBytes().map { bytes =>
        val account = Account.parseFrom(bytes)
        accounts(id) = account
        Ok
      }
    } getOrElse BadRequest

  }

}

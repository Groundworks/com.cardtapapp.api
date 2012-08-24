package controllers

import messages.Main._
import play.api.mvc._
import play.api.http.{ Writeable, ContentTypeOf }
import com.google.protobuf.Message
import play.api.libs.iteratee.Iteratee
import java.security.spec.KeySpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto._
import java.security.SecureRandom
import java.math.BigInteger
import com.google.protobuf.ByteString
import java.util.Arrays
import sun.misc.BASE64Decoder

trait DecodeAccessToken[A] extends Action[A]
object DecodeAccessToken {
  def base64decode(code:String) = new sun.misc.BASE64Decoder().decodeBuffer(code)
  def apply[A](bodyParser: BodyParser[A])(block: AccessToken => Request[A] => Result) = new DecodeAccessToken[A] {
    def parser = bodyParser
    def apply(req: Request[A]) = {
      req.headers.get("Authentication").map { auth =>
        val bytes = base64decode(auth)
        val token = AccessToken.parseFrom(bytes)
        block(token)(req)
      }getOrElse Results.Unauthorized
    }
  }
  def apply(block: AccessToken => Request[AnyContent] => Result): Action[AnyContent] = {
    DecodeAccessToken(BodyParsers.parse.anyContent)(block)
  }
}

object ClientManager {
  def poll(clientid: String) = {
    "confirmationcode"
  }
  val clients = collection.mutable.Map[String, Client]()
}

object Post extends Controller {
  def register = Action { Created }
  def share(email: String) = DecodeAccessToken { token => Action { Accepted } }
}

object Get extends Controller {

  implicit def contentTypeOf_Protobuf: ContentTypeOf[Message] = {
    ContentTypeOf[Message](Some("application/x-protobuf"))
  }
  implicit def writeableOf_Protobuf: Writeable[Message] = {
    Writeable[Message](message => message.toByteArray())
  }

  def singleItemStack = {
    Stack.newBuilder().addIndexes(Index.newBuilder()).build()
  }

  def confirm(key: String) = Action {
    Redirect("/stack")
  }

  def stack(uuid: String) = DecodeAccessToken { token => Action { Ok(singleItemStack) } }
  def card(uuid: String) = DecodeAccessToken { token => Action { Ok } }
  def inbox =  DecodeAccessToken { token => Action{ Ok(singleItemStack) }}

}

object Put extends Controller {
  def stack(uuid: String) = DecodeAccessToken { token => Action { NoContent }}
}

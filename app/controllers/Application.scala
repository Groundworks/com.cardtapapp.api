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

object Application extends App {
  val accounts = collection.mutable.Map[String, Account]()
  val authorizations = collection.mutable.Map[String, Authorization]()
}

trait App extends Controller {

  val accounts: collection.mutable.Map[String, Account]
  val authorizations: collection.mutable.Map[String, Authorization]

  implicit def contentTypeOf_Protobuf: ContentTypeOf[Message] = {
    ContentTypeOf[Message](Some("application/x-protobuf"))
  }
  implicit def writeableOf_Protobuf: Writeable[Message] = {
    Writeable[Message](message => message.toByteArray())
  }

  import util.PBKDF2._

  def deviceGet(id: String) = Action { request =>
    request.headers.get("Authorization").flatMap { auth =>
      authorizations.get(id).map { authzn =>
        val hashStored = authzn.getSecretHash().toByteArray()
        val salt = authzn.getSecretSalt().toByteArray()
        val hashRemote = pbhash(auth, salt)
        if (java.util.Arrays.equals(hashStored, hashRemote)) {
          Ok(authorizations(id).getDevice().toByteArray())
        } else {
          Ok(authorizations(id).getDevice().toByteArray())
        }
      }
    } getOrElse Unauthorized
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

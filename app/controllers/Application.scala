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
  import sun.misc.BASE64Decoder

  def deviceGet(id: String) = Action { request =>
    request.headers.get("Authorization").map { auth =>
      val bytes = new BASE64Decoder().decodeBuffer(auth)
      val creds = Credentials.parseFrom(bytes)
      if(creds.getDevice() equals id){
        authorizations.get(id).map{ auth=>
          
          val hashLocal = auth.getSecretHash().toByteArray()
          val saltLocal = auth.getSecretSalt().toByteArray()
          val passCheck = creds.getSecret()
          val hashCheck = pbhash(passCheck,saltLocal)
          
          val checked = java.util.Arrays.equals(hashLocal, hashCheck)
          if(checked){
            Ok(auth.getDevice())
          }else{
            Unauthorized
          }
          
        }getOrElse{
          NotFound
        }
      }else{
        Unauthorized
      }
    } getOrElse BadRequest
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

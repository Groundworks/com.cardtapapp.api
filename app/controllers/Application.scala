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

object Post extends Controller {
  def register = Action{Created}
  def share(email:String) = Action{Accepted}
}

object Get extends Controller {

  def confirm(key:String) = Action{Redirect("/stack")}
  def stack(uuid:String) = Action{Ok}
  def card(uuid:String) = Action{Ok}
  def inbox = Action{Ok}
  
}

object Put extends Controller {
  def stack(uuid:String) = Action{NoContent}
}

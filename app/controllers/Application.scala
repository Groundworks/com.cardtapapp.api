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
import controllers._
import controllers.Implicits._
import java.util.UUID

object ClientManager {

  val nextClientId = java.util.UUID.randomUUID().toString()

  def newClientWithEmail(email: String) = {
    val clientid = nextClientId
    clients(clientid) = Client
      .newBuilder()
      .setToken(AccessToken.newBuilder().setClientid(clientid).setClientsecret("???"))
      .setEmail(email)
      .build()
    stacks(clientid) = Stack
      .newBuilder()
      .addIndexes(Index.newBuilder().build())
      .build()
    clientid
  }

  def poll(clientid: String) = {
    "confirmationcode"
  }

  def getClientById(clientid: String) = {
    clients(clientid)
  }

  val clients = collection.mutable.Map[String, Client]()
  val inbox = collection.mutable.Map[String, Stack]()
  val stacks = collection.mutable.Map[String, Stack]()

  def getInbox(clientid: String) = {
    inbox.get(clientid).getOrElse(Stack.newBuilder().build())
  }

  def postToInbox(clientid: String, index: Index) {
    inbox.get(clientid).map { stack =>
      inbox(clientid) = Stack.newBuilder(stack).addIndexes(index).build()
    } getOrElse {
      inbox(clientid) = Stack.newBuilder().addIndexes(index).build()
    }
  }

  def putStack(clientid: String, stack: Stack) {
    stacks(clientid) = stack
  }

  def getStack(clientid: String) = {
    stacks.get(clientid).getOrElse {
      Stack.newBuilder().build()
    }
  }

}

// -- Controllers -- //

// POST //
object Post extends Controller {

  def register = DecodeProtobuf(classOf[Registration]) { registration =>
    Action {
      val token = ClientManager.getClientById(ClientManager.newClientWithEmail(registration.getEmail())).getToken()
      Created(token)
    }
  }

  def share(email: String) = DecodeProtobuf(classOf[Index]) { index =>
    DecodeAccessToken { token =>
      Action {
        val clientid = token.getClientid()
        ClientManager.postToInbox(clientid, index)
        Accepted
      }
    }
  }
}

// GET //
object Get extends Controller {

  def singleItemStack = {
    Stack.newBuilder().addIndexes(Index.newBuilder()).build()
  }

  def getCardById(id: String) = {
    Card.newBuilder().setFace("face.png").setRear("rear.png").build()
  }

  def confirm(key: String) = Action {
    Redirect("/stack")
  }

  def stack(uuid: String) = DecodeAccessToken { token =>
    val clientid = token.getClientid()
    Action {
      Ok(ClientManager.getStack(clientid))
    }
  }
  def card(uuid: String) = DecodeAccessToken { token => Action { Ok(getCardById("HI")) } }
  def inbox = DecodeAccessToken { token =>
    Action {
      val clientid = token.getClientid()
      val stack = ClientManager.getInbox(clientid)
      Ok(stack)
    }
  }
}

// PUT //
object Put extends Controller {
  def stack(uuid: String) = DecodeAccessToken { token =>
    DecodeProtobuf(classOf[Stack]) { stack =>
      val clientid = token.getClientid()
      ClientManager.putStack(clientid, stack)
      Action {
        NoContent
      }
    }
  }
}


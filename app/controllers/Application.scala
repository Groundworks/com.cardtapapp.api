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

object Repository {

  import collection.mutable.Map

  val clients = Map[String, Client]()
  val inbox = Map[String, Stack]()
  val stacks = Map[String, Stack]()
  val cards = Map[String, Card]("test" -> Card.newBuilder().setFace("face.png").setRear("rear.png").build())

  val nextUUID = java.util.UUID.randomUUID().toString()

  def postCard(card: Card) = {
    val cardid = nextUUID
    cards(cardid) = card
    cardid
  }

  def getCard(cardid: String): Option[Card] = {
    cards.get(cardid)
  }

  val defaultStack = Stack.newBuilder().addIndexes(
    Index.newBuilder().setCard("test")).build()

  def generateAccessToken(clientid:String) = {
    val clientsecret = HMac.sign(clientid.getBytes())
    AccessToken.newBuilder().setClientid(clientid).setClientsecret(clientsecret).build()
  }

  def newClientWithEmail(email: String) = {
    val clientid = nextUUID
    clients(clientid) = Client
      .newBuilder()
      .setToken(generateAccessToken(clientid))
      .setEmail(email)
      .build()
    stacks(clientid) = defaultStack
    clientid
  }

  def poll(clientid: String) = {
    "confirmationcode"
  }

  def getClientById(clientid: String) = {
    clients(clientid)
  }

  def getInbox(clientid: String) = {
    inbox.get(clientid).getOrElse(Stack.newBuilder().build())
  }

  def postToInbox(clientid: String, index: Index) {
    inbox.get(clientid).map {
      (stack =>
        inbox(clientid) = Stack.newBuilder(stack).addIndexes(index).build())
    } getOrElse {
      inbox(clientid) = Stack.newBuilder().addIndexes(index).build()
    }
  }

  def putStack(clientid: String, stack: Stack) {
    stacks(clientid) = stack
  }

  def getStack(clientid: String) = {
    stacks.get(clientid).getOrElse { defaultStack }
  }
}

// -- Controllers -- //

// POST //
object Post extends Controller {

  def register = DecodeProtobuf(classOf[Registration]) { registration =>
    Action {
      val token = Repository.getClientById(Repository.newClientWithEmail(registration.getEmail())).getToken()
      Created(token)
    }
  }

  def share(email: String) = DecodeProtobuf(classOf[Index]) { index =>
    DecodeAccessToken { clientid =>
      Action {
        Repository.postToInbox(clientid, index)
        Accepted
      }
    }
  }
}

// GET //
object Get extends Controller {

  def confirm(key: String) = Action {
    Redirect("/stack")
  }

  def stack(uuid: String) = DecodeAccessToken { clientid =>
    Action {
      Ok(Repository.getStack(clientid))
    }
  }
  def card(cardid: String) = DecodeAccessToken { token =>
    Action {
      Repository.getCard(cardid).map { card =>
        Ok(card)
      } getOrElse {
        NotFound
      }
    }
  }
  def inbox = DecodeAccessToken { clientid =>
    Action {
      val stack = Repository.getInbox(clientid)
      Ok(stack)
    }
  }
}

// PUT //
object Put extends Controller {
  def stack(uuid: String) = DecodeAccessToken { clientid =>
    DecodeProtobuf(classOf[Stack]) { stack =>
      Repository.putStack(clientid, stack)
      Action {
        NoContent
      }
    }
  }
}


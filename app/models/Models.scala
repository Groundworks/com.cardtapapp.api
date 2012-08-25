package models

import protobuf.Main._
import play.api.mvc._
import controllers._
import controllers.Implicits._

object Repository {

  import collection.mutable.Map

  val clients = Map[String, Client]()
  val inbox   = Map[String, Stack]()
  val stacks  = Map[String, Stack]()
  val cards   = Map[String, Card]("test" -> Card.newBuilder().setFace("face.png").setRear("rear.png").build())

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

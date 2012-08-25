package models

import protobuf.Main._
import play.api.mvc._
import controllers._
import controllers.Implicits._
import play.api.Play.current
import com.google.protobuf.Message

object Database {
  val default = play.api.db.DB.getConnection()
}

object Bigtable {
  import Database.{ default => db }
  def put(rowkey: String, column: String, buffer: Array[Byte]) {
    val stmt = db.prepareStatement("INSERT INTO bigtable (rowkey,column,buffer) VALUES (?,?,?)")
    stmt.setString(1, rowkey)
    stmt.setString(2, column)
    stmt.setBytes(3, buffer)
    stmt.executeUpdate()
  }
  def get(rowkey: String, column: String): Option[Array[Byte]] = {
    val stmt = db.prepareStatement("SELECT buffer FROM bigtable WHERE rowkey=? AND column=? ORDER BY version DESC")
    stmt.setString(1, rowkey)
    stmt.setString(2, column)
    val rslt = stmt.executeQuery()
    if (rslt.next()) {
      val buffer = rslt.getBytes(1)
      Some(buffer)
    } else {
      None
    }
  }
}

object Repository {

  import Bigtable._
  import collection.mutable.Map

  val CARDS = "CARDS"
  val CLIENTS = "CLIENTS"
  val STACKS = "STACKS"
  val INBOX = "INBOX"

  val nextUUID = java.util.UUID.randomUUID().toString()
  implicit def messageToBytes(message: Message) = {
    message.toByteArray()
  }

  def postCard(card: Card) = {
    val cardid = nextUUID
    put(CARDS, cardid, card)
    cardid
  }

  def getCard(cardid: String): Option[Card] = {
    if (cardid == "test") {
      Some(Card.newBuilder().setFace("face.png").setRear("rear.png").build())
    } else {
      get(CARDS, cardid).map(bytes => Card.parseFrom(bytes))
    }
  }

  val defaultStack = Stack.newBuilder().addIndexes(
    Index.newBuilder().setCard("test")).build()
    
  def generateAccessToken(clientid: String) = {
    val clientsecret = HMac.sign(clientid.getBytes())
    AccessToken.newBuilder().setClientid(clientid).setClientsecret(clientsecret).build()
  }

  def newClientWithEmail(email: String) = {
    val clientid = nextUUID
    put(CLIENTS, clientid, Client
      .newBuilder()
      .setToken(generateAccessToken(clientid))
      .setEmail(email)
      .build().toByteArray())
    put(STACKS, clientid, defaultStack)
    clientid
  }

  def poll(clientid: String) = {
    "confirmationcode"
  }

  def getClientById(clientid: String): Option[Client] = {
    val x = get(CLIENTS, clientid)
    x.map { bytes => Client.parseFrom(bytes) }
  }

  def getInbox(clientid: String) = {
    get(INBOX, clientid).map { bytes => Stack.parseFrom(bytes) }.getOrElse(Stack.newBuilder().build())
  }

  def postToInbox(clientid: String, index: Index) {
    get(INBOX, clientid).map { bytes =>
      val stack = Stack.parseFrom(bytes)
      put(INBOX,clientid,Stack.newBuilder(stack).addIndexes(index).build().toByteArray())
    } getOrElse {
      put(INBOX,clientid,Stack.newBuilder().addIndexes(index).build().toByteArray())
    }
  }

  def putStack(clientid: String, stack: Stack) {
    put(STACKS,clientid,stack.toByteArray())
  }

  def getStack(clientid: String) = {
    get(STACKS,clientid).map{bytes=>Stack.parseFrom(bytes)}.getOrElse { defaultStack }
  }
}

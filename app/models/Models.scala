package models

import protobuf.Main._
import play.api.mvc._
import controllers._
import controllers.Implicits._
import play.api.Play.current

object Database {
  val default = play.api.db.DB.getConnection()
}

object Bigtable {
  import Database.{default=>db}
  def put(rowkey:String,column:String,buffer:Array[Byte]){
    val stmt = db.prepareStatement("INSERT INTO bigtable (rowkey,column,buffer) VALUES (?,?,?)")
    stmt.setString(1, rowkey)
    stmt.setString(2, column)
    stmt.setBytes(3, buffer)
    stmt.executeUpdate()
  }
  def get(rowkey:String,column:String): Option[Array[Byte]]={
    val stmt = db.prepareStatement("SELECT buffer FROM bigtable WHERE rowkey=? AND column=? ORDER BY version DESC")
    stmt.setString(1,rowkey)
    stmt.setString(2,column)
    val rslt = stmt.executeQuery()
    if(rslt.next()){
      val buffer = rslt.getBytes(1)
      Some(buffer)
    }else{
      None
    }
  }
}

object Repository {

  import collection.mutable.Map

  val clients = Map[String, Client]()
  val inbox   = Map[String, Stack]()
  val stacks  = Map[String, Stack]()
  val cards   = Map[String, Card]()

  val nextUUID = java.util.UUID.randomUUID().toString()

  def postCard(card: Card) = {
    val cardid = nextUUID
    cards(cardid) = card
    cardid
  }

  def getCard(cardid: String): Option[Card] = {
    if (cardid=="test"){
      Some(Card.newBuilder().setFace("face.png").setRear("rear.png").build())
    }else{
      cards.get(cardid) 
    }
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

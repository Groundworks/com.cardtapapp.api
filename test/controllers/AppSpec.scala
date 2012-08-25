package controllers

import messages.Main._
import com.google.protobuf.Message
import org.scalatest._
import org.scalatest.GivenWhenThen._
import org.scalatest.matchers.ShouldMatchers._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._
import sun.misc.BASE64Encoder

object CardTap {

  val POST = "POST"
  val GET = "GET"
  val PUT = "PUT"

  val nil = AnyContentAsRaw(RawBuffer(1, Array[Byte]()))

  implicit def headers: Map[String, Seq[String]] = Map()

  def base64encode(bytes: Array[Byte]) = new sun.misc.BASE64Encoder().encode(bytes)
  def authHeader(token: AccessToken) = {
    val value = base64encode(token.toByteArray())
    Map("Authentication" -> Seq(value))
  }

  def get(path: String)(implicit headers: Map[String, Seq[String]]) = {
    val uri = "http://cardtapapp.test%s" format path
    new FakeRequest(
      GET,
      uri,
      new FakeHeaders(headers),
      AnyContentAsEmpty)
  }

  def post(path: String, body: AnyContent)(implicit headers: Map[String, Seq[String]]) = {
    val uri = "http://cardtapapp.test%s" format path

    new FakeRequest(
      POST,
      uri,
      new FakeHeaders(headers),
      body)
  }

  def put(path: String, body: AnyContent)(implicit headers: Map[String, Seq[String]]) = {
    val uri = "http://cardtapapp.test%s" format path

    new FakeRequest(
      PUT,
      uri,
      new FakeHeaders(headers),
      body)
  }

  def getStack(clientid: String)(implicit token: AccessToken) = {
    val res = Get.stack(null)(get("/stack/" + clientid)(authHeader(token)))
    if (status(res) equals OK) {
      Some(Stack.parseFrom(contentAsBytes(res)))
    } else {
      None
    }
  }

  def getInbox(clientid: String)(implicit token: AccessToken) = {
    val res = Get.inbox(get("/inbox/" + clientid)(authHeader(token)))
    status(res) should equal(OK)
    Stack.parseFrom(contentAsBytes(res))
  }

  def register(email: String) = {
    val reg = Registration.newBuilder().setEmail(email).build()
    val res = Post.register(post("/register", reg))
    status(res) should equal(CREATED)
    AccessToken.parseFrom(contentAsBytes(res))
  }

  def shareIndex(card: Index, withEmail: String)(implicit token: AccessToken) = Post.share(null)(post("/share/" + withEmail, card)(authHeader(token)))

  implicit def bufferToRawContent(message: Message): AnyContent = {
    val buffer = RawBuffer(1024 * 1024, message.toByteArray())
    AnyContentAsRaw(buffer)
  }

}

class AppSpec extends FeatureSpec {

  import CardTap._

  val email = "bob@example.com"

  feature("Golden Workflow") {

    // Registration //
    when("User Registers a New Device")
    implicit val token = register(email)
    token should not be null
    token.getClientid() should not be null
    token.getClientid.length() should be > 0
    token.getClientsecret() should not be null
    token.getClientsecret().length() should be > 0

    val clientid = token.getClientid()
    val clientsecret = token.getClientsecret()
    val authcode = Repository.poll(clientid)

    // Confirmation //
    then("Email should be set in client manager")
    Repository.getClientById(clientid).getEmail() should equal(email)

    then("User confirms the new registration")

    {
      val res = Get.confirm(null)(get("/confirm/" + authcode))
      status(res) should equal(303)
    }

    // Access Content //
    then("User gets stack of cards")

    when("Access token is not presented")

    {
      status(Get.stack(null)(get("/stack/" + clientid))) should equal(UNAUTHORIZED)
    }
    then("Acesss is denied")

    when("Access token is invalid")

    {
      implicit val token = AccessToken.newBuilder().setClientid(clientid).setClientsecret("???").build()
      getStack(clientid) should equal(None)
    }

    then("Access is denied")

    when("Access token is included")

    {

      val stack = getStack(clientid).get
      stack should not be null
      stack.getIndexesCount() should be > 0

      then("User device downloads cards in stack")

      for (i <- 0 until stack.getIndexesCount()) {
        val cardid = stack.getIndexes(i).getCard()
        val res = Get.card(cardid)(get("/card/" + cardid)(authHeader(token)))
        status(res) should equal(OK)
        val card = Card.parseFrom(contentAsBytes(res))

        card should not be null
        card.getFace().length() should be > 0
        card.getRear().length() should be > 0
        then("Downloaded Card should contain urls to face and rear images")
      }

    }
    // Sharing //
    then("User is shared a card by another user")

    {
      val altToken = register("kim@example.com")
      val card = Card.newBuilder().setFace("FF").setRear("RR").build()
      val cardid = Repository.postCard(card)
      val index = Index.newBuilder().setCard(cardid).build()
      val N = getInbox(clientid).getIndexesCount()
      shareIndex(index, email)(altToken)
      getInbox(clientid).getIndexesCount() should equal(N + 1)
    }

    when("User downloads inbox")

    {
      val inbox = getInbox(clientid)
      inbox.getIndexesCount() should be > 0
    }

    then("User shares a card with another user")

    {
      val card = Card.newBuilder().setFace("FACE-1").setRear("REAR-1").build()
      val cardid = Repository.postCard(card)
      val withEmail = "tom@example.com"
      val index = Index.newBuilder().setCard(cardid).build()
      val res = shareIndex(index, withEmail)
      status(res) should equal(ACCEPTED)
    }

    // Updates //
    then("User deletes a card")

    {
      val stack0 = Stack.newBuilder().build()
      val res = Put.stack(null)(put("/stack/" + clientid, stack0)(authHeader(token)))
      status(res) should equal(NO_CONTENT)

      val stack1 = getStack(clientid)
      stack1.get.getIndexesCount() should equal(0)
    }

  }

}
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

class AppSpec extends FeatureSpec {

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

  implicit def bufferToRawContent(message: Message): AnyContent = {
    val buffer = RawBuffer(1024 * 1024, message.toByteArray())
    AnyContentAsRaw(buffer)
  }

  val email = "bob@example.com"

  feature("Golden Workflow") {

    when("User Registers a New Device")

    val token = {
      val reg = Registration.newBuilder().setEmail(email).build()
      val res = Post.register(post("/register", reg))
      status(res) should equal(CREATED)
      AccessToken.parseFrom(contentAsBytes(res))
    }
    token should not be null

    val clientid = token.getClientid()
    val clientsecret = token.getClientsecret()
    val authcode = ClientManager.poll(clientid)

    then("User confirms the new registration")

    {
      val res = Get.confirm(null)(get("/confirm/" + authcode))
      status(res) should equal(303)
    }

    then("User gets stack of cards")

    when("Access token is not presented")
    status(Get.stack(null)(get("/stack/" + clientid))) should equal(UNAUTHORIZED)
    then("Acesss is denied")
    
    when("Access token is included")
    val stack = {
      val res = Get.stack(null)(get("/stack/" + clientid)(authHeader(token)))
      status(res) should equal(OK)
      Stack.parseFrom(contentAsBytes(res))
    }
    stack should not be null
    stack.getIndexesCount() should be > 0

    then("User device downloads cards in stack")

    for (i <- 0 until stack.getIndexesCount()) {
      val cardid = stack.getIndexes(i).getCard()
      val res = Get.card(cardid)(get("/card/" + cardid)(authHeader(token)))
      status(res) should equal(OK)
      val card = Card.parseFrom(contentAsBytes(res))
      card should not be null
    }

    then("User is shared a card by another user")

    val inbox = {
      val res = Get.inbox(get("/inbox/" + clientid)(authHeader(token)))
      status(res) should equal(OK)
      Stack.parseFrom(contentAsBytes(res))
    }
    inbox.getIndexesCount() should be > 0

    then("User shares a card with another user")

    {
      val withEmail = "tom@example.com"
      val cardid = "???"
      val share = Index.newBuilder().setCard(cardid).build()
      val res = Post.share(null)(post("/share/" + withEmail, share)(authHeader(token)))
      status(res) should equal(ACCEPTED)
    }

    then("User deletes a card")

    {
      val stack = Stack.newBuilder().build()
      val res = Put.stack(null)(put("/stack/" + clientid, stack)(authHeader(token)))
      status(res) should equal(NO_CONTENT)
    }

  }

}
package controllers

import messages.Main._
import com.google.protobuf.Message
import org.scalatest._
import org.scalatest.GivenWhenThen._
import org.scalatest.matchers.MustMatchers._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._

class AppSpec extends FeatureSpec {

  val POST = "POST"
  val GET = "GET"
  val PUT = "PUT"

  val nil = AnyContentAsRaw(RawBuffer(1, Array[Byte]()))

  implicit def headers: Map[String, Seq[String]] = Map()

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
      status(res) must equal(CREATED)
      AccessToken.parseFrom(contentAsBytes(res))
    }
    
    val clientid     = token.getClientid()
    val clientsecret = token.getClientsecret()
    val authcode     = ClientManager.poll(clientid)
    
    then("User confirms the new registration")
    
    {
      val res = Get.confirm(null)(get("/confirm/" + authcode))
      status(res) must equal(303)
    }
    
    then("User gets stack of cards")
    
    val stack = {
      val res = Get.stack(null)(get("/stack/" + clientid))
      status(res) must equal(OK)
      Stack.parseFrom(contentAsBytes(res))
    }
    stack.getIndexesCount() must be > 0
    
    then("User is shared a card by another user")
    
    val inbox = {
      val res = Get.inbox(get("/inbox/" + clientid))
      status(res) must equal(OK)
      Stack.parseFrom(contentAsBytes(res))
    }
    inbox.getIndexesCount() must be > 0
    
    then("User shares a card with another user")

    {
      val withEmail = "tom@example.com"
      val cardid = "???"
      val share = Index.newBuilder().setCard(cardid).build()
      val res = Post.share(null)(post("/share/" + withEmail, share))
      status(res) must equal(ACCEPTED)
    }

    then("User deletes a card")
    
    {
      val stack = Stack.newBuilder().build() 
      val res = Put.stack(null)(put("/stack/" + clientid, stack))
      status(res) must equal(NO_CONTENT)
    }

  }

}
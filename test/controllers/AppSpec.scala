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
  val nil = AnyContentAsRaw(RawBuffer(1, Array[Byte]()))

  implicit def headers: Map[String, Seq[String]] = Map()

  def post(path: String, body: AnyContent)(implicit headers: Map[String, Seq[String]]) = {
    val uri = "http://cardtapapp.test%s" format path

    new FakeRequest(
      POST,
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
    
    {
      val reg = Registration.newBuilder().setEmail(email).build()
      val res = Post.register(post("/register", reg))
      status(res) must equal(CREATED)
    }

    then("User confirms the new registration")
    
    {
      val res = Get.confirm(null)(FakeRequest())
      status(res) must equal(303)
    }

    then("User gets stack of cards")
    
    {
      val res = Get.stack(null)(FakeRequest())
      status(res) must equal(OK)
    }

    then("User is shared a card by another user")
    
    {
      val res = Get.inbox(FakeRequest())
      status(res) must equal(OK)
    }

    then("User shares a card with another user")
    
    {
      val res = Post.share(null)(FakeRequest())
      status(res) must equal(ACCEPTED)
    }

    then("User deletes a card")
    
    {
      val res = Put.stack(null)(FakeRequest())
      status(res) must equal(NO_CONTENT)
    }

  }

}
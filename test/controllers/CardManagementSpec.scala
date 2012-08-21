package controllers

import com.cardtapapp.api.Main._
import org.scalatest.FeatureSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.GivenWhenThen._
import org.junit.runner.RunWith
import dispatch._
import org.scalatest.matchers.ShouldMatchers._

@RunWith(classOf[JUnitRunner])
class ManagementSpec extends FeatureSpec {

  import api.Web._

  def addCard(secret: String, card: Card) {
    Http(url(host + "/card/" + secret)
      .setHeader("Content-Type", "application/x-protobuf")
      .POST
      .setBody(card.toByteArray()))()
  }

  feature("Card Management") {

    scenario("User Deletes a Card from their Stack") {

      val secret = device("test")
      val stack0 = account(secret).getStack()

      val count0 = account(secret).getStack().getCardsCount()
      post("/card/" + secret)("face" -> "1.png", "rear" -> "2.png")
      val count1 = account(secret).getStack().getCardsCount()
      count1 should equal(count0 + 1)

      val res = Http(url(host + "/stack/" + secret).POST.setBody(stack0.toByteArray()))()
      res.getStatusCode() should equal(201)
      val count2 = account(secret).getStack().getCardsCount()
      count2 should equal(count0)

    }

    scenario("User Adds a Card Twice it Does Not Appear More than Once") {
      when("User adds a card object twice")
      val secret = device("test")
      val stack0 = account(secret).getStack()

      val count0 = stack0.getCardsCount()

      val card = Card
        .newBuilder()
        .setUuid(java.util.UUID.randomUUID().toString)
        .build()

      addCard(secret, card)
      addCard(secret, card)

      val stack1 = account(secret).getStack()
      val count1 = stack1.getCardsCount()

      then("The card only appears once in the stack")
      count1 should equal(count0 + 1)

    }

    scenario("User Marks a Card as Accepted from their Stack") {
      val secret = device("test")
      val stack0 = account(secret).getStack()

      val card = Card
        .newBuilder()
        .setStatus("accepted")
        .build()

      val svc = url(host + "/card/" + secret)
        .addHeader("Content-Type", "application/x-protobuf")
        .POST
        .setBody(card.toByteArray())
      val res = Http(svc)()
      res.getStatusCode() should equal(201)

      val stack = account(secret).getStack()
      val N = stack.getCardsCount()

      var checked = false
      for (i <- 0 until N) {
        val card = stack.getCards(i)
        if (card.getUuid() == "asdf") {
          card.getStatus() should equal("accepted")
          checked = true
        }
      }
      checked should equal(true)

    }

  }

}

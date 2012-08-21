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

  feature("Card Management") {

    scenario("User Deletes a Card from their Stack") {

      val secret = device("test")
      val stack0 = account(secret).getStack()
      
      val count0 = account(secret).getStack().getCardsCount()
      post("/card/" + secret)("face" -> "1.png", "rear" -> "2.png")
      val count1 = account(secret).getStack().getCardsCount()
      count1 should equal( count0 + 1 )
      
      val res = Http(url(host+"/stack/" + secret).POST.setBody(stack0.toByteArray()))()
      res.getStatusCode() should equal(201)
      val count2 = account(secret).getStack().getCardsCount()
      count2 should equal( count0 )
      
    }
  }

}

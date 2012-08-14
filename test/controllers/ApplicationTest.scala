package controllers

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import com.dd.plist._
import play.api.libs.ws._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dd.plist.NSDictionary

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Users API" should {
    "specity an application/plist type" in {
      running(FakeApplication()) {
        val result = controllers.Application.user("demo")(FakeRequest())
        contentType(result) must beSome("application/plist")
      }
    }
    "return an NSDictionary" in {
      running(FakeApplication()) {
        val result = controllers.Application.user("demo")(FakeRequest())
        val nsobject = PropertyListParser.parse(contentAsBytes(result))
        val nsdict = nsobject.asInstanceOf[NSDictionary]

        nsdict must not be null
      }
    }
    "return an NSDictionary with an array of cards" in {
      running(FakeApplication()) {
        val result = controllers.Application.user("demo")(FakeRequest())
        val nsobject = PropertyListParser.parse(contentAsBytes(result))
        val nsdict = nsobject.asInstanceOf[NSDictionary]

        val cardarray = nsdict.objectForKey("Cards").asInstanceOf[NSArray]
        
        cardarray must not be null
      }
    }
  }
}

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
class RegistrationSpec extends Specification {

  "Users API" should {
    "return 201 created" in {
      running(FakeApplication()) {
        val device = "fja03fj093jf2390jf23f"
        val user   = "bob@email.com"
        
        val result   = controllers.Application.register(device,user)(FakeRequest())
        status(result) must equalTo(201)
      }
    }
    "include the location of the registered user" in {
      running(FakeApplication()) {
        val device = "fja03fj093jf2390jf23f"
        val user   = "bob@email.com"
        
        val result   = controllers.Application.register(device,user)(FakeRequest())
        headers(result).get("Location").get must equalTo("/user/bob@email.com")
      }
    }
    "disallow double registration" in {
      val device = "fja03fj093jf2390jf23f"
      val user   = "bob@email.com"
      
      val result1 = controllers.Application.register(device,user)(FakeRequest())
      val result2 = controllers.Application.register(device,user)(FakeRequest())
      
      status(result2) must not be equalTo(201)
    }
  }
}

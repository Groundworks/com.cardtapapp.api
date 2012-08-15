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

  def reg = FakeRequest(POST, "/register")

  "Registration" should {
    "Expect Form Data" in {
      val Some(result) = routeAndCall(reg)

      status(result) must equalTo(400)
    }
    "Expect Email" in {
      val Some(result) = routeAndCall(reg.withFormUrlEncodedBody("bob" -> "Smith"))

      status(result) must equalTo(400)
    }
    "Include Location on Success" in {
      val Some(result) = routeAndCall(reg.withFormUrlEncodedBody("email" -> "bob"))
      
      header("Location",result) must not beNone
    }
    "201 Create on Success" in {
      val Some(result) = routeAndCall(reg.withFormUrlEncodedBody("email" -> "bob"))

      status(result) must equalTo(201)
      header("Location", result) must not be equalTo(None)
    }
  }

  "Authorization" should {
    "Return 404 for unknown" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/authorize/1234"))

      status(result) must equalTo(404)
    }
    "Exist after registration" in {
      val Some(result) = routeAndCall(reg.withFormUrlEncodedBody("email" -> "bob"))
      controllers.Application.authorization.map { item =>
        val authKey = item._1
        val Some(result2) = routeAndCall(FakeRequest(GET, "/authorize/" + authKey))

        status(result2) must beLessThan(399)
      } must not be empty
    }
    "Redirect after success" in {
      val Some(result) = routeAndCall(reg.withFormUrlEncodedBody("email" -> "bob"))
      val Some(location) = header("Location", result)
      val device = location.split("/")(2)
      controllers.Application.registration.get(device).map { registration =>
        val Some(result2) = routeAndCall(FakeRequest(GET, "/authorize/" + registration.authcode))
        status(result2) must be equalTo(303)
        header("Location",result2).get must be equalTo("/login/"+device)
      } must not beNone
    }
  }

  "Login" should {
    "Not Found for Missing Device" in {
      val Some(result) = routeAndCall(FakeRequest(GET,"/login/abc"))
      
      status(result) must be equalTo(404)
    }
    "Fail without authorization" in {
      routeAndCall(FakeRequest(POST, "/register").withFormUrlEncodedBody("email" -> "bob")).map { result =>
        val redirect = header("Location", result).get
        val Some(loginResult) = routeAndCall(FakeRequest(GET, redirect))

        status(loginResult) must equalTo(401)

      } must not be equalTo(None)
    }
    "Succeed After Aurhotirzation" in {
      val Some(result) = routeAndCall(reg.withFormUrlEncodedBody("email" -> "bob"))
      val Some(location) = header("Location", result)
      val device = location.split("/")(2)
      println("Getting Device Key From Location Url: %s" format device)
      controllers.Application.registration.get(device).map { registration =>
        val authKey = registration.authcode
        val Some(result2) = routeAndCall(FakeRequest(GET, "/authorize/" + authKey))
        val Some(result3) = routeAndCall(FakeRequest(GET, header("Location", result).get))
        status(result3) must equalTo(200)

      } must not be empty
    }
  }

}

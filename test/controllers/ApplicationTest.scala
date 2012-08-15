package controllers

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import com.dd.plist._
import play.api.libs.ws._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.dd.plist.NSDictionary
import java.net.URL

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  def reg = FakeRequest(POST, "/register")

  def register = routeAndCall(reg.withFormUrlEncodedBody("email" -> "bob")).get

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
      header("Location", register) must not beNone
    }
    "201 Create on Success" in {
      status(register) must equalTo(201)
    }
  }

  "Authorization" should {
    "Return 404 for unknown" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/authorize/1234"))

      status(result) must equalTo(404)
    }
    "Redirect after registration" in {
      register
      controllers.Application.authorization.map { item =>
        val authKey = item._1
        val Some(result2) = routeAndCall(FakeRequest(GET, "/authorize/" + authKey))

        status(result2) must beEqualTo(303)
      } must not be empty
    }
    "Redirect to custom scheme after success" in {
      val Some(location) = header("Location", register)
      val device = location.split("/")(2)
      controllers.Application.registration.get(device).map { registration =>
        val Some(result2) = routeAndCall(FakeRequest(GET, "/authorize/" + registration.authcode))
        val url = header("Location", result2).get

        url must be equalTo ("cardtapapp+http://localhost/login/" + device)
      } must not beNone
    }
  }

  "Login" should {
    "Not Found for Missing Device" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/login/abc"))

      status(result) must be equalTo (404)
    }
    "Fail without authorization" in {
      val redirect = header("Location", register).get
      val Some(loginResult) = routeAndCall(FakeRequest(GET, redirect))

      status(loginResult) must equalTo(401)
    }
    "Succeed After Aurhotirzation" in {
      val result = register
      val Some(location) = header("Location", result)
      val device = location.split("/")(2)
      println("Getting Device Key From Location Url: %s" format device)
      controllers.Application.registration.get(device).map { registration =>
        val authKey = registration.authcode
        val Some(result2) = routeAndCall(FakeRequest(GET, "/authorize/" + authKey))
        val Some(result3) = routeAndCall(FakeRequest(GET, header("Location",result).get))
        status(result3) must equalTo(200)

      } must not be empty
    }
  }

}

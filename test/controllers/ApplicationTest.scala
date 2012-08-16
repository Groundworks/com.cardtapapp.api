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

  def register = routeAndCall(reg.withFormUrlEncodedBody("email" -> "test")).get

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
  implicit def resultToDeviceKey(result: play.api.mvc.Result): String = {
    header("Location", result).get.split("/")(2)
  }
  def authorize(device: String): play.api.mvc.Result = {
    controllers.Application.registration.get(device).map { registration =>
      routeAndCall(FakeRequest(GET, "/authorize/" + registration.authcode)).get
    }.get
  }
  "Authorization" should {
    "Return 404 for unknown" in {
      val Some(result) = routeAndCall(FakeRequest(GET, "/authorize/1234"))
      status(result) must equalTo(404)
    }
    "Redirect after authorization" in {
      val result = authorize(register)
      status(result) must beEqualTo(303)
    }
    "Redirect to custom scheme after success" in {
      val reg = register
      val dev = resultToDeviceKey(reg)
      val url = header("Location", authorize(dev)).get
      url must be equalTo ("cardtapapp+http://localhost/login/" + dev)
    }
  }

  def loginWithAuthorization(reg:play.api.mvc.Result) = {
    val aut = authorize(reg)
    val dev = resultToDeviceKey(reg)
    routeAndCall(FakeRequest(GET, "/login/" + dev))
  }

  def userDataWithAuthorization(reg:play.api.mvc.Result) = {
    loginWithAuthorization(reg).map { result =>
      PropertyListParser.parse(contentAsBytes(result))
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
      authorize(result)
      val Some(result2) = routeAndCall(FakeRequest(GET, header("Location", result).get))
      status(result2) must equalTo(200)
    }
    "Return Content-Type application/plist on Success" in {
      val Some(result) = loginWithAuthorization(register)
      contentType(result) must be beSome ("application/plist")
    }
    "Return a Plist" in {
      userDataWithAuthorization(register) must not beNull
    }
  }

  "Login Data" should {
    "be an NSDictionary" in {
      val plist = userDataWithAuthorization(register).get
      plist must haveClass[NSDictionary]
    }
    "contain user email" in {
      val dict = userDataWithAuthorization(register).get.asInstanceOf[NSDictionary]
      val emailObj = dict.objectForKey("Email")
      emailObj must haveClass[NSString]
    }
    "contain registered email" in {
      val email = "XYZ"
      val Some(res) = routeAndCall( FakeRequest(POST,"/register").withFormUrlEncodedBody("email"->email) )
      userDataWithAuthorization(res).map{ result =>
        val dict = result.asInstanceOf[NSDictionary]
        val nsstring = dict.objectForKey("Email").asInstanceOf[NSString]
        nsstring.toString() must beEqualTo(email)
      } must not beNone
    }
    "contain an array of cards" in {
      var dict = userDataWithAuthorization(register).get.asInstanceOf[NSDictionary]
      var cards = dict.objectForKey("Cards").asInstanceOf[NSArray]
      cards must not beNull
    }
  }
}

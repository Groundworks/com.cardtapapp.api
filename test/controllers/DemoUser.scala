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
class DemoUserSpec extends Specification {

  "Demo User" should {
    "Have 5 cards" in {
      
      val reg = Registration("demo","demo-device","demo-authcode",true)
      
      controllers.Application.registration("demo-device")    = reg
      controllers.Application.authorization("demo-authcode") = reg
      
      val Some(result) = routeAndCall(FakeRequest(GET,"/login/demo-device"))
      val dict = PropertyListParser.parse(contentAsBytes(result)).asInstanceOf[NSDictionary]
      
      val cards = dict.objectForKey("Cards").asInstanceOf[NSArray]
      
      cards.count() must equalTo(5)
    }
  }

}
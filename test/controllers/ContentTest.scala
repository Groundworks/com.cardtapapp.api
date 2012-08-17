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

import com.cardtapapp.api.Main._

object MockDataStore extends DataStore {
  def getAccountByEmail(email:String):Option[Account] = {
    val account = Account.newBuilder().setEmail(email).setUuid("").build()
    Some(account)
  }
  def setAccount(account:Account){
    
  }
  def setShare(share:Share){
    
  }
  def getCardByUuid(uuid:String):Option[Card]={
    None
  }
  def setCard(card:Card){
    
  }
}

object TestApplication extends ApplicationFramework {
  val dataStore = MockDataStore 
  val registration  = collection.mutable.Map[String,Registration]("bob"->Registration("bob", "bob", "bob", true))
  val authorization = collection.mutable.Map[String,Registration]()
} 

@RunWith(classOf[JUnitRunner])
class ContentsSpec extends Specification {
  
  "Content" should {
    "Return with Email bob" in {
      val result = TestApplication.login("bob")(FakeRequest())
      val bytes  = contentAsBytes(result)
      val account = Account.parseFrom( bytes )
      
      account.getEmail() must equalTo("bob")
    }
  }
 
}
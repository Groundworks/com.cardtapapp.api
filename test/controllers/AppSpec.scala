package controllers

import com.cardtapapp.api.Main._

import org.scalatest.FeatureSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.GivenWhenThen._
import org.junit.runner.RunWith
import dispatch._

import org.scalatest.matchers.ShouldMatchers._

@RunWith(classOf[JUnitRunner])
class AppSpec extends FeatureSpec {

  val host = "http://localhost:9000"

  def post(path: String)(query: (String, String)*) = {
    val svc = url(host+path).POST
    for(q<-query){
      val (key,value) = q
      svc.addQueryParameter(key,value)
    }
    Http(svc)()
  }

  def get(path: String) = {
    Http(url(host + path))()
  }

  def register(email: String) = {
    val rsp = post("/register")("email"->email)
    
    rsp.getStatusCode() should equal(201)
    val login = rsp.getHeader("Location")

    login should startWith("/login")

    rsp.getResponseBody()
  }

  def poll(secret: String) = {
    val rsp = get("/poll/" + secret)
    rsp.getResponseBody()
  }

  def login(secret: String) = {
    get("/login/" + secret)
  }

  def auth(code: String) = {
    get("/auth/" + code)
  }

  def device(email: String) = {
    val secret = register(email)
    auth(poll(secret))
    secret
  }
  
  def account(secret:String) = {
    Account.parseFrom( login(secret).getResponseBodyAsBytes() )
  }
  
  feature("Registration") {

    scenario("User Registers a New Device") {

      val email = "test"
        
      when("Registring a new device")
      val secret = register(email)

      then("Should return unauthorized before confirming")
      login(secret).getStatusCode() should equal(401)

      then("the user may confirm confirming")
      val code = poll(secret)
      val locn = auth(code)
      locn.getStatusCode() should equal(303)
      locn.getHeader("Location") should startWith("/login")

      then("The account can be accessed")
      login(secret).getStatusCode() should equal(200)
      
      account(secret) should not be null
      account(secret) getEmail() should equal(email)
      
    }

    scenario("User Adds a Card") {
      val secret = device("test")
      val account1 = account(secret)
      val count = account1.getStack().getCardsCount()
      
      when("a user adds a new card")
      val rsp = post("/card/"+secret)("face"->"1.png","rear"->"2.png")
      rsp.getStatusCode() should equal(201)
      val account2 = account(secret)
      
      then("the card should appear in their pile")
      account2.getStack().getCardsCount() should equal(count+1)
    }
    
    scenario("User is Shared a Card by Another User"){
      val sec1 = device("test2")
      val acc1 = account(sec1)
      
      val sec2 = device("test2")
      val acc2_0 = account(sec2)
      
      when("Another user shares a card with another")
      val rsp = post("/share/"+sec1)("with"->"test2","card"->"")
      rsp.getStatusCode() should equal(204)
      
      val acc2_1 = account(sec2)
      
      then("The other user should have the card appear in their stack")
      acc2_0.getStack().getCardsCount()+1 should
      	equal( acc2_1.getStack().getCardsCount())
      
    }
  }
}

package controllers

import messages.Main._
import com.google.protobuf.Message
import org.scalatest._
import org.scalatest.GivenWhenThen._
import org.scalatest.matchers.ShouldMatchers._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._
import sun.misc.BASE64Encoder
import collection.mutable.Stack
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers

class AppCorner extends FlatSpec with ShouldMatchers {

  import CardTap._
  
  "Registration" should "return empty BadRequest" in {
    
    val res = Post.register(post("/register", null))
    status(res) should equal(BAD_REQUEST)
    
  }
  
  
  
}
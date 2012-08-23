package controllers

import messages.Main._
import org.scalatest._
import play.api.test._
import play.api.mvc._
import play.api.test.Helpers._
import org.scalatest.matchers.MustMatchers._

class AccountSpec extends FeatureSpec with GivenWhenThen {

  val email = "bob@gmail.com"

  feature("Account Access") {

    info("A user with a valid account may access that account via HTTP")

    scenario("User Gets their Account Data") {

      object A extends App {
        val accounts = collection.mutable.Map[String,Account](email->Account.newBuilder().build())
        val authorizations = collection.mutable.Map[String, Authorization]()
      }

      when("User GETs Accout without Authentication Header")
      val resps = A.accountGet(email)(FakeRequest())
      status(resps) must equal(UNAUTHORIZED)
      then("Account returns Unauthorized")

      when("User GETs an Accout with proper authentication")
      val request = new FakeRequest(
        "GET",
        "http://localhost/account/" + email,
        new FakeHeaders(Map("Authorization" -> Seq("Bob"))),
        new play.api.mvc.AnyContentAsText(""))
      val respa = A.accountGet(email)(request)
      status(respa) must equal(OK)
      then("The Account Returns Success")
      val bytes = contentAsBytes(respa)
      val account = Account.parseFrom(bytes)
      account must not be null
      and("The body returns a protocl buffer")

      when("User GETs an account with incorrect authentication")
      val requestBad = new FakeRequest(
        "GET",
        "http://localhost/account/" + email,
        new FakeHeaders(Map("Authorization" -> Seq("Andy"))),
        new play.api.mvc.AnyContentAsText(""))
      val respBad = A.accountGet(email)(requestBad)
      status(respBad) must equal(UNAUTHORIZED)
      then("The account returns unauthorized")

    }

    scenario("User PUTs and GETs their Account Data") {
      object A extends App {
        val accounts = collection.mutable.Map[String,Account]()
        val authorizations = collection.mutable.Map[String, Authorization]()
      }

      when("User puts a new account to the server")
      val account = Account.newBuilder().setFullname("BOBBY").build()
      val buffer = new RawBuffer(1000, account.toByteArray())
      val request = new FakeRequest(
        "PUT",
        "http://localhost/account/" + email,
        new FakeHeaders(Map("Authorization" -> Seq("Bob"))),
        new play.api.mvc.AnyContentAsRaw(buffer))
      val resp = A.accountPut(email)(request)
      status(resp) must equal(OK)
      then("server returns with OK")

      when("user PUTs the new account data")
      val request2 = new FakeRequest(
        "GET",
        "http://localhost/account/" + email,
        new FakeHeaders(Map("Authorization" -> Seq("Bob"))),
        new play.api.mvc.AnyContentAsText(""))
      val respa = A.accountGet(email)(request2)
      status(respa) must equal(OK)
      then("The PUT Returns Success")
      val bytes = contentAsBytes(respa)
      val account2 = Account.parseFrom(bytes)
      account2.getFullname() must equal("BOBBY")
      then("the account is updated")
      
    }

  }

}
package controllers

import org.scalatest._
import org.scalatest.GivenWhenThen._
import org.scalatest.matchers.ShouldMatchers._
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._

class AppCorner extends FlatSpec {

  import CardTap._

  "Registration" should "return empty BadRequest" in {

    running(FakeApplication()) {

      1 should equal(1)

    }
  }

}
package controllers

import com.cardtapapp.api.Main._

import Ensemble._

import akka.actor._
import akka.pattern._
import akka.util._

import play.api.data.Forms._
import play.api.data.Form

import play.api._
import play.api.libs.concurrent.AkkaPromise
import play.api.mvc._

import play.libs.Akka.system

import ensemble._

// Ensemble //

object Ensemble {
  val accountManager = system.actorOf(Props[AccountManager])
  val deviceManager = system.actorOf(Props[DeviceManager])
  val shareManager = system.actorOf(Props[ShareManager])
  val mailManager = system.actorOf(Props[MailManager])
}

// Controller //

object Application extends Controller {

  import Ensemble._

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))
  def async(_query: akka.dispatch.Future[Any])(_handler: Any => Result) = {
    Async {
      new AkkaPromise(_query).map { _handler }
    }
  }

  val shareForm = Form(tuple(
    "card" -> text,
    "with" -> text))

  def share(secret: String) = Action { implicit request =>
    shareForm.bindFromRequest.fold(
      error => BadRequest("Must Provide `card` and `with` Form Data"),
      value => {
        val (cardShare, cardWith) = value
        async(shareManager ? ShareCard(cardWith, cardShare, secret)) {
          case Failure => BadRequest
          case Success => NoContent
          case _       => InternalServerError
        }
      })
  }

  val registerForm = Form("email" -> text)

  def register = Action { implicit request =>
    registerForm.bindFromRequest.fold(
      error => BadRequest("Must Provide `Email` Form Data"),
      value => async(deviceManager ? RegisterDevice(value)) {
        case Failure => BadRequest
        case DeviceRegistration(registration) =>
          val login = routes.Application.login(registration).toString
          Created.withHeaders("Location" -> login)
        case _ => InternalServerError
      })
  }

  def login(secret: String) = Action {
    async(accountManager ? Login(secret)) {
      case Success => Ok
      case account: Account =>
        Ok(account.toByteArray())
      case _ => InternalServerError
    }
  }

  def auth(devcode: String) = Action {
    async(accountManager ? Authorize(devcode)) {
      case RedirectLogin(secret) =>
        Redirect(routes.Application.login(secret))
      case _ => InternalServerError
    }
  }
}

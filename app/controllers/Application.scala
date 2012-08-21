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
import com.googlecode.protobuf.format.XmlFormat
import com.googlecode.protobuf.format.JsonFormat

// Ensemble //

object Ensemble {
  val accountManager = system.actorOf(Props[AccountManager])
  val deviceManager = system.actorOf(Props[DeviceManager])
  val shareManager = system.actorOf(Props[ShareManager])
  val mailManager = system.actorOf(Props[MailManager])
  val cardManager = system.actorOf(Props[CardManager])
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

  val cardForm = Form(tuple("face" -> text, "rear" -> text))
  def card(secret: String) = Action { implicit request =>
    cardForm.bindFromRequest.fold(
      error => BadRequest,
      value => {
        val (face, rear) = value
        val card = Card.newBuilder()
          .setUuid(java.util.UUID.randomUUID().toString)
          .setImageFace(face)
          .setImageRear(rear)
          .build()

        val x = for {
          email <- (deviceManager ? GetRegisteredEmail(secret)).mapTo[String]
          reslt <- (accountManager ? AddCardToAccount(email, card))
        } yield reslt

        val z = async(x) {
          case Success => Created
          case _       => InternalServerError
        }

        z
      })
  }

  val shareForm = Form(tuple(
    "card" -> text,
    "with" -> text))

  def share(secret: String) = Action { implicit request =>
    shareForm.bindFromRequest.fold(
      error => BadRequest("Must Provide `card` and `with` Form Data"),
      value => {
        val (cardShare, cardWith) = value
        val card = Card.newBuilder().setImageFace("1.png").setImageRear("2.png").build()
        async(shareManager ? ShareCard(cardWith, card, secret)) {
          case AccountNotFound     => NotFound("Account Error")
          case DeviceNotRegistered => NotFound("Device Error")
          case DeviceNotAuthorized => Unauthorized
          case Success             => NoContent
          case _                   => InternalServerError
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
      case account: Account =>
        val body = JsonFormat.printToString(account)
        Ok(body)
      case AccountNotAuthorized =>
        Unauthorized
      case AccountNotFound =>
        NotFound
      case _ => InternalServerError
    }
  }

  def auth(devcode: String) = Action {
    async(accountManager ? Authorize(devcode)) {
      case RedirectLogin(secret) =>
        Redirect(routes.Application.login(secret))
      case Failure =>
        NotFound
      case _ => InternalServerError
    }
  }
}


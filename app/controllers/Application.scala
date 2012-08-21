package controllers

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import akka.util._
import play.api.data.Forms._
import play.api.data.Form
import play.api._
import play.api.libs.concurrent.AkkaPromise
import play.api.mvc._
import play.libs.Akka.system
import actors._
import com.googlecode.protobuf.format.XmlFormat
import com.googlecode.protobuf.format.JsonFormat
import controllers.log.logger

package object log {
    
  val logger = play.api.Logger("controllers")
}

// Controller //

object Application extends Controller {

  val director = system.actorOf(Props[Director], "main")

  {
    director ! Start
    logger.debug("Director Has Path: %s" format director.path)
  }

  def devicesManager = system.actorFor("user/main/devices")
  def accountManager = system.actorFor("user/main/accounts")
  def sharingManager = system.actorFor("user/main/shares")

  logger.debug("Device Manager Reference at Path: %s" format devicesManager.path)

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))
  def async(_query: akka.dispatch.Future[Any])(_handler: Any => Result) = {
    Async {
      new AkkaPromise(_query).map { _handler }
    }
  }

  def stack(secret: String) = Action { request =>
    request.body.asRaw.flatMap { buffer =>
      buffer.asBytes() map { bytes =>
        val stack = Stack.parseFrom(bytes)
        async(devicesManager ? GetAccountFromSecretIfAuthorized(secret)) {
          case account: Account =>
            async(accountManager ? UpdateStackForAccount(account, stack)) {
              case Success => Created
              case _       => InternalServerError
            }
          case DeviceNotAuthorized => Unauthorized
          case _                   => InternalServerError
        }
      }
    } getOrElse BadRequest
  }

  val cardForm = Form(tuple("face" -> text, "rear" -> text))
  def card(secret: String) = Action { implicit request =>
    // Process Request Differently Depending on Content-Type //
    // ----------------------------------------------------- //
    // This content is processed functionally as a chain of  //
    // monads propagating through maps and flat maps.
    // Errors are handled at the end via a single BadRequest //
    request.contentType.flatMap {
      case "application/x-protobuf" =>
        // Deserialize Protbufs Directly //
        logger.debug("Card Parsing Incoming Protocol Buffer")
        request.body.asRaw flatMap { raw =>
          raw.asBytes().map { bytes =>
            Card.parseFrom(bytes)
          }
        }
      case _ =>
        // Non-Protobuf, Try Prasing as Form Data //
        logger.debug("Card Parsing Incoming Form Data")
        cardForm.bindFromRequest.fold(
          error => {
            logger.warn("Card Cannot Bind Form Data")
            None
          },
          value => {
            val (face, rear) = value
            Some(Card.newBuilder()
              .setUuid(java.util.UUID.randomUUID().toString)
              .setImageFace(face)
              .setImageRear(rear)
              .build())
          })
    } map { card =>
      // Called if we receive Some(card) from the previous block
      // Otherwise this is not called  
      async(for {
        email <- (devicesManager ? GetRegisteredEmail(secret)).mapTo[String]
        reslt <- (accountManager ? AddCardToAccount(email, card))
      } yield reslt) {
        case Success => Created
        case _       => InternalServerError
      }
    } getOrElse BadRequest
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
        async(sharingManager ? ShareCard(cardWith, card, secret)) {
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
      value => async(devicesManager ? RegisterNewDevice(value)) {
        case Failure => BadRequest
        case DeviceRegistration(registration) =>
          val login = routes.Application.login(registration).toString
          Created(registration).withHeaders("Location" -> login)
        case _ => InternalServerError
      })
  }

  def login(secret: String) = Action {
    async(devicesManager ? LoginWithDeviceSecret(secret)) {
      case account: Account     => Ok(account.toByteArray())
      case AccountNotAuthorized => Unauthorized
      case AccountNotFound      => NotFound
      case _                    => InternalServerError
    }
  }

  def auth(devcode: String) = Action {
    async(devicesManager ? AuthorizeRegisteredDevice(devcode)) {
      case RedirectLogin(secret) => Redirect(routes.Application.login(secret))
      case Failure               => NotFound
      case _                     => InternalServerError
    }
  }

  def poll(secret: String) = Action {
    async(devicesManager ? GetAuthorizationCodeFromSecret(secret)) {
      case code: String => Ok(code)
      case _            => InternalServerError
    }
  }

  def status = Action { Ok }

}


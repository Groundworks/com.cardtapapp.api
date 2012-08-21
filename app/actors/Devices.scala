package actors

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import play.api.Logger
import controllers.Random._
import akka.util.Timeout
import akka.util.Duration

// Devices //

case class GetDevice(secret: String)
case class RegisterDevice(email: String)
case class DeviceRegistration(secret: String)
case class GetRegisteredEmail(secret: String)
case class GetAuthorizationCodeFromSecret(secret: String)

case object DeviceNotRegistered
case object DeviceNotAuthorized

class DeviceManager extends Actor {

  import models.AuthorizationAccesses._

  def isValid(auth: Authorization) = {
    auth.getAccess() equals AUTH_VALIDATED
  }

  def validateEmail(auth: Authorization) = {
    if (isValid(auth)) {
      Some(auth.getEmail())
    } else {
      None
    }
  }

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  import models.DevicesModel._

  val accountManager = context.actorFor("../accounts")
  val mailManager = context.actorFor("../mailer")

  Logger.debug("Device Manager ActorRef to AccountManager has Path: %s" format accountManager.path)

  def receive = {

    case GetAuthorizationCodeFromSecret(secret: String) =>
      getAuthorizationFromSecret(secret) map { auth =>
        sender ! auth.getCode()
      }

    case GetRegisteredEmail(secret: String) =>
      getAuthorizationFromSecret(secret) map { auth =>
        sender ! auth.getEmail()
      } getOrElse {
        sender ! None
      }

    case RegisterDevice(email) =>
      Logger.info("Registering New Device to Email: " + email)

      val auth = newAuthorizationFromEmail(email)

      accountManager ! EnsureAccount(email)
      mailManager ! RegistrationConfirmation(auth)
      sender ! DeviceRegistration(auth.getDevice().getSecret())

    case Authorize(code) =>

      getAuthorizationFromCode(code) map { authzn =>
      	
        // Authorize Registered Device //
        
        val authok = Authorization.newBuilder(authzn).setAccess(AUTH_VALIDATED).build()
        setAuthorizationFromCode(code, authok)

        sender ! RedirectLogin(device.getSecret())

      } getOrElse {
        sender ! Failure
      }

    case Login(secret) =>
      Logger.info("Attempt Log In with Secret: " + secret)
      getAuthorizationFromSecret(secret) map { authzn =>
        var s = sender // avoid closure over actor internals

        validateEmail(authzn) map { email =>
          accountManager ? GetAccount(email) map {
            case Some(account: Account) => s ! account
            case _                      => s ! Failure
          }
        } getOrElse {
          Logger.info("Login Fail - Device %s Not Yet Authorized" format secret)
          s ! AccountNotAuthorized
        }

      } getOrElse { sender ! Failure }

    case any =>
      Logger.warn("Unknown Message Received by Device Manager: " + any)
      sender ! Failure
  }

}

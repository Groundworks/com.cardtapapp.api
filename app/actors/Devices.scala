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
case class RegisterNewDevice(email: String)
case class DeviceRegistration(secret: String)
case class GetRegisteredEmail(secret: String)
case class GetAuthorizationCodeFromSecret(secret: String)
case class LoginWithDeviceSecret(secret: String)
case class AuthorizeRegisteredDevice(code: String)

case object DeviceNotRegistered
case object DeviceNotAuthorized

class DeviceManager extends Actor {
  
  val AUTH_VALIDATED = "VALIDATED"
  val AUTH_PENDING = "PENDING"

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

  val accountManager = context.actorFor("../accounts")
  val mailersManager = context.actorFor("../mailer")

  Logger.debug("Device Manager ActorRef to AccountManager has Path: %s" format accountManager.path)

  import models.DevicesModel._
  
  def receive = {

    case GetAuthorizationCodeFromSecret(secret: String) =>
      getAuthorizationFromSecret(secret) map { auth =>
        sender ! auth.getCode()
      }

    case GetRegisteredEmail(secret: String) =>
      Logger.debug("Get Registered Email from Secret: %s" format secret)
      getAuthorizationFromSecret(secret) map { auth =>
        sender ! auth.getEmail()
      } getOrElse {
        sender ! None
      }

    case RegisterNewDevice(email) =>
      Logger.debug("Registering New Device to Email: " + email)
      val auth = newAuthorizationFromEmail(email)
      accountManager ! EnsureAccountExists(email)
      mailersManager ! RegistrationConfirmation(auth)
      sender ! DeviceRegistration(auth.getDevice().getSecret())

    case AuthorizeRegisteredDevice(code) =>
      Logger.debug("Authorizing Device with Code: " + code)
      getAuthorizationFromCode(code) map { authzn =>
        val authok = Authorization.newBuilder(authzn).setAccess(AUTH_VALIDATED).build()
        setAuthorizationFromCode(code, authok)
        sender ! RedirectLogin(device.getSecret())
      } getOrElse {
        sender ! Failure
      }

    case LoginWithDeviceSecret(secret) =>
      Logger.debug("Log In with Device Secret: " + secret)
      getAuthorizationFromSecret(secret) map { authzn =>
        var s = sender // avoid closure over actor internals
        validateEmail(authzn) map { email =>
          accountManager ? GetAccount(email) map {
            case Some(account: Account) => s ! account
            case _                      => s ! Failure
          }
        } getOrElse {
          Logger.warn("Login Fail - Device %s Not Yet Authorized" format secret)
          s ! AccountNotAuthorized
        }

      } getOrElse { sender ! Failure }

    case any =>
      Logger.warn("Unknown Message Received by Device Manager: " + any)
      sender ! Failure
  }

}

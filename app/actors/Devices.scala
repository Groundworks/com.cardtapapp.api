package actors

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import actors.log.logger
import controllers.Random._
import akka.util.Timeout
import akka.util.Duration

// Devices //

case class GetAccountFromSecretIfAuthorized(secret: String)
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

  logger.debug("Device Manager ActorRef to AccountManager has Path: %s" format accountManager.path)

  import models.DevicesModel._

  def receive = {

    case GetAccountFromSecretIfAuthorized(secret) =>
      val s = sender
      getAuthorizationFromSecret(secret) map { auth =>
        if (auth.getAccess() equals AUTH_VALIDATED) {
          accountManager ? GetAccount(auth.getEmail()) map {
            case Some(account: Account) =>
              s ! account
            case _ => s ! Failure
          }
        } else {
          s ! DeviceNotAuthorized
        }
      }

    case GetDevice(secret) =>
      getAuthorizationFromSecret(secret) map { auth =>
        sender ! auth
      } getOrElse {
        logger.warn("Device Could Not be Found from Secret: " + secret)
        sender ! Failure
      }

    case GetAuthorizationCodeFromSecret(secret: String) =>
      getAuthorizationFromSecret(secret) map { auth =>
        sender ! auth.getCode()
      }

    case GetRegisteredEmail(secret: String) =>
      val s = sender
      logger.debug("Get Registered Email from Secret: %s" format secret)
      getAuthorizationFromSecret(secret) map { auth =>
        s ! auth.getEmail()
      } getOrElse {
        s ! Failure
      }

    case RegisterNewDevice(email) =>
      logger.debug("Registering New Device to Email: " + email)
      val auth = newAuthorizationFromEmail(email)
      accountManager ! EnsureAccountExists(email)
      mailersManager ! RegistrationConfirmation(auth)
      sender ! DeviceRegistration(auth.getDevice().getSecret())

    case AuthorizeRegisteredDevice(code) =>
      val s = sender
      logger.debug("Authorizing Device with Code: " + code)
      getAuthorizationFromCode(code) map { authzn =>
        val authok = Authorization.newBuilder(authzn).setAccess(AUTH_VALIDATED).build()
        setAuthorizationFromCode(code, authok)
        s ! RedirectLogin(device.getSecret())
      } getOrElse {
        s ! Failure
      }
      
    case LoginWithDeviceSecret(secret) =>
      logger.debug("Log In with Device Secret: " + secret)
      getAuthorizationFromSecret(secret) map { authzn =>
        var s = sender // avoid closure over actor internals
        validateEmail(authzn) map { email =>
          accountManager ? GetAccount(email) map {
            case Some(account: Account) => s ! account
            case _                      => s ! Failure
          }
        } getOrElse {
          logger.warn("Login Fail - Device %s Not Yet Authorized" format secret)
          s ! AccountNotAuthorized
        }

      } getOrElse { sender ! Failure }
  }
}

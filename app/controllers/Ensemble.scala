package ensemble;

import play.api._
import akka.actor._
import com.cardtapapp.api.Main._
import utility.Random._

// General Messages //

case object Success
case object Failure

// Mailer //

case class RegistrationConfirmation(email: String, auth: Authorization)
case class ShareNotification(email: String, card: Card)

class MailManager extends Actor {
  def receive = {
    case RegistrationConfirmation(email, auth) =>
      Logger.info("Register Device %s to %s" format (
        auth.getDevice().getSecret(), email))
      Logger.info("Confirm: /auth/" + auth.getCode())
    case any => 
      Logger.warn("Unknown Message Received by Mail Manager: " + any )
      sender ! Failure
  }
}

// Accounts //

case class GetAccount(email: String)
case class SetAccount(account: Account)
case class Login(secret: String)
case class Authorize(code: String)
case class RedirectLogin(secret: String)

class AccountManager extends Actor {
  def receive = {
    case Authorize(code) =>
      // Lookup Authorizations by Code
      Logger.info("Authorizing Code: " + code)
      sender ! RedirectLogin("a123")
    case Login(secret) =>
      Logger.info("Log In with Secret: " + secret)
      sender ! Account.newBuilder().setUuid(uuid).build()
    case any =>
      Logger.warn("Unknown Message Received by Account Manager: " + any )
      sender ! Failure
  }
}

// Devices //
case class GetDevice(secret: String)
case class RegisterDevice(email: String)
case class DeviceRegistration(secret: String)

class DeviceManager extends Actor {

  import controllers.Ensemble.mailManager

  def device = Device
    .newBuilder()
    .setUuid(uuid)
    .setSecret(uuid)
    .build()
    
  def authorization(email:String) = Authorization
    .newBuilder()
    .setUuid(uuid)
    .setCode(uuid)
    .setDevice(device)
    .setEmail(email)
    .build()

  def receive = {
    case RegisterDevice(email) =>
      Logger.info("Registering New Device to Email: "+email)
      mailManager ! RegistrationConfirmation(email, authorization(email))
      sender      ! DeviceRegistration("a123")
    case any =>
      Logger.warn("Unknown Message Received by Device Manager: " + any )
      sender ! Failure
  }

}

// Share //

case class ShareCard(email: String, card: String, secret: String)

class ShareManager extends Actor {
  def receive = {
    case ShareCard(cardWith, cardShare, "a123") => 
      Logger.info("New Care Shared to Email: "+cardWith)
      sender ! Success
    case any => 
      Logger.warn("Unknown Message Received by Share Manager: " + any )
      sender ! Failure
  }
}


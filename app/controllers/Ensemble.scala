package ensemble;

import play.api.Logger
import akka.actor._
import com.cardtapapp.api.Main._
import utility.Random._
import models.Database.{connection=>db}

import models._

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
      
      val auth = authorization(email)
      
      // Database //
      val prep = db.prepareStatement("INSERT INTO device (secret,device,buffer) VALUES (?,?,?)")
      prep.setString(1,auth.getCode())
      prep.setString(2,auth.getDevice().getSecret())
      prep.setBytes (3,auth.toByteArray() )
      val pk = prep.executeUpdate()
      Logger.debug("Insert New into Database with Primary Key: %d" format pk)
      // -------- //
      
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


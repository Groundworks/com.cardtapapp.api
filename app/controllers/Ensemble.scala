package ensemble;

import play.api.Logger
import akka.actor._
import com.cardtapapp.api.Main._
import controllers.Random._
import models.Database.{ connection => db }

// General Messages //

case object Success
case object Failure

// Mailer //

case class RegistrationConfirmation(auth: Authorization)
case class ShareNotification(email: String, card: Card)

class MailManager extends Actor {
  def receive = {
    case RegistrationConfirmation(auth) =>
      Logger.info("Register Device %s to %s" format (
        auth.getDevice().getSecret(), auth.getEmail()))
      Logger.info("Confirm: /auth/" + auth.getCode())
    case any =>
      Logger.warn("Unknown Message Received by Mail Manager: " + any)
      sender ! Failure
  }
}

// Accounts //

case class GetAccount(email: String)
case class SetAccount(account: Account)
case class Login(secret: String)
case class Authorize(code: String)
case class RedirectLogin(secret: String)
case class RegisterAccount(email: String)

case object AccountNotAuthorized
case object AccountNotFound

class AccountManager extends Actor {
  
  val AUTH_VALIDATED = "VALIDATED"
  val AUTH_PENDING   = "PENDING" 
  
  def receive = {

    case RegisterAccount(email) =>
      val stmt = db.prepareStatement("SELECT COUNT(id) FROM account WHERE email=?")
      stmt.setString(1, email)
      Logger.debug(stmt.toString())
      val rslt = stmt.executeQuery()
      if (rslt.next()) {
        val count = rslt.getInt(1)
        if (count == 0) {
          val stmt2 = db.prepareStatement("INSERT INTO account (buffer,email) VALUES (?,?)")
          stmt2.setBytes(1, Account.newBuilder().setEmail(email).setUuid(uuid).build().toByteArray())
          stmt2.setString(2, email)
          Logger.debug(stmt2.toString())
          val pk = stmt2.executeUpdate()
          Logger.info("Created New Account: %s" format email)
        }
      } else {
        Logger.warn("Error Creating New Account: %s" format email)
      }

    case Authorize(code) =>

      // Database //
      val stmt = db.prepareStatement("SELECT buffer FROM device WHERE authcode=?")
      stmt.setString(1, code)
      Logger.debug(stmt.toString())
      val rslt = stmt.executeQuery()
      Logger.debug(stmt.toString())

      // Results //
      if (rslt.next()) {
    	
        val buffer = rslt.getBytes(1)
        val authzn = Authorization.parseFrom(buffer)
        val device = authzn.getDevice()
        
        // Device is authorized - Insert into Database
        val authok = Authorization.newBuilder(authzn).setAccess(AUTH_VALIDATED).build()
        val stmt = db.prepareStatement("UPDATE device SET buffer=? WHERE authcode=?")
        stmt.setBytes(1, authok.toByteArray())
        stmt.setString(2, code)
        Logger.debug(stmt.toString())
        val rtn = stmt.executeUpdate()
        Logger.debug("Update Device Affected %d Rows in Database" format rtn)

        val loc = device.getSecret()
        Logger.info("Authorizing Code: " + code)

        sender ! RedirectLogin(device.getSecret())
      } else {
        sender ! Failure
      }

    case Login(secret) =>

      Logger.info("Attempt Log In with Secret: " + secret)

      // Database //
      val stmt = db.prepareStatement("SELECT buffer FROM device WHERE device=?")
      stmt.setString(1, secret)
      Logger.debug(stmt.toString())

      val rslt = stmt.executeQuery()
      if (rslt.next()) {
        val buffer = rslt.getBytes(1)
        val authzn = Authorization.parseFrom(buffer)

        Logger.debug("Authorization Status: %s" format authzn.getAccess())
        if (authzn.getAccess() equals AUTH_VALIDATED) {
          val email = authzn.getEmail()
          val stmt2 = db.prepareStatement("SELECT buffer FROM account WHERE email=?")

          stmt2.setString(1, email)
          Logger.debug(stmt2.toString())

          val rslt2 = stmt2.executeQuery()

          if (rslt2.next()) {
            val account = Account.parseFrom(rslt2.getBytes(1))
            sender ! account
          } else {
            Logger.warn("Account %s Not Found in Database" format email)
            sender ! Failure
          }
        } else {
          Logger.info("Login Fail - Device %s Not Yet Authorized" format secret)
          sender ! AccountNotAuthorized
        }
      } else {
        Logger.warn("Device %s Not Found in Database" format secret)
        sender ! Failure
      }
    case any =>
      Logger.warn("Unknown Message Received by Account Manager: " + any)
      sender ! Failure
  }
}

// Devices //
case class GetDevice(secret: String)
case class RegisterDevice(email: String)
case class DeviceRegistration(secret: String)

class DeviceManager extends Actor {

  import controllers.Ensemble._

  def device = Device
    .newBuilder()
    .setUuid(uuid)
    .setSecret(uuid)
    .build()

  def authorization(email: String) = Authorization
    .newBuilder()
    .setUuid(uuid)
    .setCode(uuid)
    .setDevice(device)
    .setEmail(email)
    .build()

  def receive = {
    case RegisterDevice(email) =>
      Logger.info("Registering New Device to Email: " + email)
      accountManager ! RegisterAccount(email)

      val auth = authorization(email)

      // Database //
      val prep = db.prepareStatement("INSERT INTO device (authcode,device,buffer) VALUES (?,?,?)")
      prep.setString(1, auth.getCode())
      prep.setString(2, auth.getDevice().getSecret())
      prep.setBytes(3, auth.toByteArray())
      Logger.debug(prep.toString())
      val pk = prep.executeUpdate()
      // -------- //

      mailManager ! RegistrationConfirmation(auth)
      sender ! DeviceRegistration(auth.getDevice().getSecret())
    case any =>
      Logger.warn("Unknown Message Received by Device Manager: " + any)
      sender ! Failure
  }

}

// Share //

case class ShareCard(email: String, card: String, secret: String)

class ShareManager extends Actor {
  def receive = {
    case ShareCard(cardWith, cardShare, "a123") =>
      Logger.info("New Care Shared to Email: " + cardWith)
      sender ! Success
    case any =>
      Logger.warn("Unknown Message Received by Share Manager: " + any)
      sender ! Failure
  }
}


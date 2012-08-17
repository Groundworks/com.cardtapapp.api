package controllers

import anorm._

import com.dd.plist._

import com.cardtapapp.api.Main._

import play.api._
import play.api.mvc._

import java.util.UUID
import play.api.db.DB
import play.api.Play.current

case class Registration(
  email: String,
  device: String,
  authcode: String,
  var authorized: Boolean)

trait DataStore {
  def getAccountByEmail(email: String): Option[Account];
  def setAccount(account: Account);
  def setShare(share: Share);
  def getCardByUuid(uuid: String): Option[Card];
  def setCard(card: Card);
  def setDevice(dev: Device);
  def getDeviceBySecret(secret: String): Option[String];
}

object Application extends ApplicationFramework {
  val dataStore = null
}

trait ApplicationFramework extends Controller {

  val dataStore: DataStore

  def share = Action { request =>
    request.body.asFormUrlEncoded.map { form =>
      val device = form.get("device").get.head

      val card = form.get("card").get.head
      val recipient = form.get("recipient").get.head

      // TODO

      NoContent
    }.getOrElse { BadRequest }
  }

  def sendEmailAuthorization(email: String, code: String) {
    Logger.info("Sending Authorization Code %s" format code)
  }

  def register = Action { implicit request =>
    request.body.asFormUrlEncoded.map { form =>
      form.get("email").map { emails =>

        val email = emails.head
        val uuid = UUID.randomUUID().toString()
        val auth = UUID.randomUUID().toString()
        
        val dev = Device.newBuilder()
          .setAuthorized(false)
          .setSecret(auth)
          .setUuid(uuid)
          .setAccountid(email)
          .build()
        
        dataStore.setDevice(dev)
        
        Logger.info("User %s Registered Device %s with Authorization Code %s" format (email, uuid, auth))

        Created.withHeaders("Location" -> routes.Application.login(uuid).toString)
      }.getOrElse { BadRequest("Expecting Email in Form Data") }
    }.getOrElse {
      BadRequest("Expecting Form Data")
    }
  }

  def authorize(devkey: String, authcode: String) = Action { request =>
    Logger.info("Authorizing with Code %s" format authcode)
    .get(authcode).map { registration =>
      registration.authorized = true
      val path = routes.Application.login(registration.device).toString
      val prot = "cardtapapp+http"
      val host = if (request.host == "") { "localhost" } else { request.host }
      val redirect = "%s://%s%s" format (prot, host, path)
      Logger.debug("Redirecting to %s" format redirect)
      Redirect(redirect)
    }.getOrElse { NotFound }
  }

  def login(device: String) = Action {
    Logger.info("Attempting Device %s Log In" format device)
    registration.get(device).map { registration =>
      if (registration.authorized) {

        Logger.info("Device %s Login Success" format device)
        val email = registration.email
        val account = dataStore.getAccountByEmail(email).get
        Ok(account.toByteArray()).withHeaders("Content-Type" -> "application/octet-stream")

      } else {
        Logger.info("Device %s Not Authorized" format device)
        Unauthorized("This Device Has Not Been Authorized")
      }
    }.getOrElse { NotFound("") }
  }

  def dump = Action {
    val regs = registration.foldLeft("\n")((out, next) => out + next + "\n") +
      authorization.foldLeft("\n")((out, next) => out + next + "\n")
    Ok(regs)
  }

}

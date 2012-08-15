package controllers

import play.api._
import play.api.mvc._

import com.dd.plist._

object Application extends Controller {

  def user(userkey: String) = Action { request =>

    Logger.info("Get User Account: %s" format userkey)
    Logger.debug(request.headers.toSimpleMap.foldLeft("Headers:\n") { case (x, (y, z)) => x + y + ":" + z + "\n" })

    if (userkey == uid) {
      Ok(views.html.user()).withHeaders("Content-Type" -> "application/plist")
    } else {
      NotFound("")
    }
  }

  val uid = "437"
  val key = "e29"
  val dev = "85e"

  def poll(devkey: String) = Action {
    Logger.info("Polling for Device: %s" format devkey)
    if (confirmed) {
      if (devkey == dev) {
        confirmed = false
        Redirect(routes.Application.user(uid))
      } else {
        NotFound("")
      }
    } else {
      Forbidden("Please Confirm Device by Email")
    }
  }

  def sendEmailConfirmation(email: String) {
    Logger.info("Sending Email Confirmation to %s" format email)
    Logger.info("To Confirm Please Follow the Location: %s" format routes.Application.confirm(key))
  }
  
  def scrubEmail(email: String): Option[String] = email match {
    case "" => None
    case email => Some(email)
  }
  
  def associateNewEmail(email:String): String = {
    Logger.info("Associating New Email %s with Key %s" format (email,dev))
    dev
  }
  
  def register = Action { request =>
    request.body.asFormUrlEncoded.map { form =>
      form.get("email").map { emails =>
        emails.headOption.map { dirtyemail =>
          scrubEmail(dirtyemail).map { email =>
            
            // Success //
            val device = associateNewEmail(email)
            Logger.info("Register New Device: %s to User: %s" format (device, email))
            sendEmailConfirmation(email)
            Redirect(routes.Application.poll(dev))
            
          }.getOrElse{BadRequest("Not a valid email")}
        }.getOrElse{BadRequest("No Valid Email Recieved")}
      }.getOrElse{BadRequest("Registration must include an email") }
    }.getOrElse{BadRequest("Registration type must be application/form-url-encoded") }
  }
  
  var confirmed = false
  def confirm(confirmKey: String) = Action { request =>
    if (confirmKey == key) {
      confirmed = true
      Logger.info("Confirmed Key: %s" format confirmKey)
      Redirect("cardtapapp+http://" + request.headers("Host") + "/poll/" + dev)
    } else {
      NotFound("")
    }
  }

  def index = Action {
    Redirect("http://cardtapapp.com")
  }

}

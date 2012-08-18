package controllers

import com.cardtapapp.api.Main._
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.libs.Akka.system
import java.util.UUID
import play.api.Play.current
import play.api.libs.concurrent.AkkaPromise
import akka.actor.{Actor,ActorRef,Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.util.Duration
import play.libs.Akka.system
import play.libs.Akka

case object Success
case object Failure

class MailManager extends Actor {
  def receive = {
    case _ => sender ! Success 
  }
}

class AccountManager extends Actor {
  def receive = {
    case _ => sender ! Success
  }
}

object Application extends Controller {

  val accountManager = system.actorOf(Props[AccountManager])
  val mailManager = system.actorOf(Props[MailManager]) 

  def uuid = UUID.randomUUID().toString()
  
  implicit val timeout : Timeout = Timeout(Duration(5,"seconds"))
  def share(secret: String) = Action { request =>
    if (secret == "a123") {
      request.body.asFormUrlEncoded.flatMap { form =>
        form.get("card").flatMap { card =>
          form.get("with").map { email =>
            Async {
              new AkkaPromise( mailManager ? "" ) map {
                case _ => Ok
              }
            }
          }
        }
      } getOrElse BadRequest
    } else NotFound
  }
  
  def register = Action { request =>
    request.body.asFormUrlEncoded.flatMap { form =>
      form.get("email").map { emailseq =>
        val secret = "a123"
        val redirect = "/login/" + secret
        Created(secret).withHeaders("Location" -> redirect)
      }
    } getOrElse BadRequest
  }

  def login(secret: String) = Action {
    if (secret == "a123") {
      Ok
    } else NotFound
  }

  def auth(devcode: String) = Action {
    val code = "zzz"
    if (devcode == code) {
      Ok
    } else NotFound
  }
}

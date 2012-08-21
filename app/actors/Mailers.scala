package actors

import akka.actor._
import com.cardtapapp.api.Main._
import play.api.Logger

// Mailer //

case class RegistrationConfirmation(auth: Authorization)
case class ShareNotification(email: String, card: Card)

class MailManager extends Actor {
  def receive = {
    case ShareNotification(email, card) =>
      Logger.info("Share Card with Email: %s" format email)
    case RegistrationConfirmation(auth) =>
      Logger.info("Register Device %s to %s" format (
        auth.getDevice().getSecret(), auth.getEmail()))
      Logger.info("Confirm: /auth/" + auth.getCode())
  }
}

package actors

import akka.actor._
import com.cardtapapp.api.Main._
import actors.log.logger

// Mailer //

case class RegistrationConfirmation(auth: Authorization)
case class ShareNotification(email: String, card: Card)

class MailManager extends Actor {
  def receive = {
    case ShareNotification(email, card) =>
      logger.info("Share Card with Email: %s" format email)
    case RegistrationConfirmation(auth) =>
      logger.info("Register Device %s to %s" format (
        auth.getDevice().getSecret(), auth.getEmail()))
      logger.info("Confirm: /auth/" + auth.getCode())
  }
}

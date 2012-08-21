package actors
import akka.actor._
import actors.log.logger

// General Messages //

case object Success
case object Failure

// Ensemble //

case object Start

class Director extends Actor {
  def receive = {
    case Start =>
      Ensemble.start(context)
    case _     => logger.debug("Director Received Message")
  }
}

object Ensemble {

  def start(context: ActorContext) {
    val accounts = context.actorOf(Props[AccountManager], "accounts")
    val devices  = context.actorOf(Props[DeviceManager], "devices")
    val shares   = context.actorOf(Props[ShareManager], "shares")
    val mailers  = context.actorOf(Props[MailManager], "mailer")
    
    logger.debug("Accounts Actor Has path: %s" format accounts.path)
    logger.debug("Devices Actor Has path: %s" format devices.path)
    logger.debug("Shares Actor Has path: %s" format shares.path)
    logger.debug("Mailers Actor Has path: %s" format mailers.path)
  }

}


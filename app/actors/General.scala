package actors
import akka.actor._
import play.api.Logger

// General Messages //

case object Success
case object Failure

// Ensemble //

case object Start

class Director extends Actor {
  def receive = {
    case Start =>
      Ensemble.start(context)
    case _     => Logger.debug("Director Received Message")
  }
}

object Ensemble {

  def start(context: ActorContext) {
    val accounts = context.actorOf(Props[AccountManager], "accounts")
    val devices  = context.actorOf(Props[DeviceManager], "devices")
    val shares   = context.actorOf(Props[ShareManager], "shares")
    val mailers  = context.actorOf(Props[MailManager], "mailer")
    
    Logger.debug("Accounts Actor Has path: %s" format accounts.path)
    Logger.debug("Devices Actor Has path: %s" format devices.path)
    Logger.debug("Shares Actor Has path: %s" format shares.path)
    Logger.debug("Mailers Actor Has path: %s" format mailers.path)
  }

}


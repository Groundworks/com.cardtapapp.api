package actors

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import actors.log.logger
import util.Random._
import util.HMac
import akka.util.Timeout
import akka.util.Duration

case class ProcessShare(share: Share, secret: String)

case object HMacFailure

class ShareManager extends Actor {

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  // Message Relative Actors in the Ensemble // 
  val accountManager = context.actorFor("../accounts")
  val devicesManager = context.actorFor("../devices")

  import models.SharesModel._

  def receive = {

    case ProcessShare(share, secret) =>
      val s = sender // avoid closure over outer scope

      val sign = HMac.sign(share.getCard().getBundle().toByteArray())
      val hash = share.getCard().getHmac()

      logger.debug("Bundle Signature: %s" format sign)
      logger.debug("Card HMac Signature: %s" format hash)

      if (sign == hash) {
        // Get Account from Devices Actor //
        devicesManager ? GetAccountFromSecretIfAuthorized(secret) map {
          case account: Account =>
            newShare(share)

            // Tell Accounts Actor to Modify Account //
            accountManager ? AddCardToStack(share.getWith(), share.getCard()) map {
              case Success =>
                logger.info("Card Shared to %s" format share.getWith())
                s ! Success
              case _ =>
                logger.warn("Share Card Failed to Add Card to Account")
                s ! Failure
            }
          case _ =>
            logger.warn("Share Card Received Unknown Message from Device Manager")
            s ! _
        }
      } else {
        logger.warn("Shared Card Not Authorized by Signature")
        s ! HMacFailure
      }
  }
}

package actors

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import actors.log.logger
import controllers.Random._
import akka.util.Timeout
import akka.util.Duration

case class ShareCard(email: String, card: Card, secret: String)

class ShareManager extends Actor {

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  // Message Relative Actors in the Ensemble // 
  val accountManager = context.actorFor("../accounts")
  val devicesManager = context.actorFor("../devices")

  import models.SharesModel._

  def receive = {

    case ShareCard(shareWith, card, secret) =>
      val s = sender // avoid closure over outer scope
      val sharedCard = Card
      	.newBuilder(card)
      	.setStatus("shared")
      	.build()
      
      // Get Account from Devices Actor //
      devicesManager ? GetAccountFromSecretIfAuthorized(secret) map {
        case account: Account =>
          val share = Share.newBuilder()
            .setCard(sharedCard)
            .setWith(shareWith)
            .setFrom(account.getEmail())
            .build()
          newShare(share)
          
          // Tell Accounts Actor to Modify Account //
          accountManager ? AddCardToAccount(shareWith, card) map {
            case Success => s ! Success
            case _ =>
              logger.warn("Share Card Failed to Add Card to Account")
              s ! Failure
          }
        case _ =>
          logger.warn("Share Card Received Unknown Message from Device Manager")
          s ! _
      }
  }
}

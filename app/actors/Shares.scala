package actors

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import play.api.Logger
import controllers.Random._
import akka.util.Timeout
import akka.util.Duration

// Share //

case class ShareCard(email: String, card: Card, secret: String)

class ShareManager extends Actor {

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  val accountManager = context.actorFor("../accounts")
  val devicesManager = context.actorFor("../devices")

  import models.SharesModel._
  
  def receive = {

    case ShareCard(shareWith, card, secret) =>

      val s = sender // avoid closure over outer scope

      devicesManager ? GetAccountFromSecretIfAuthorized(secret) map {
        case account: Account =>
          val share = Share.newBuilder()
            .setCard(card)
            .setWith(shareWith)
            .setFrom(account.getEmail())
            .build()
          newShare(share)
          accountManager ? AddCardToAccount(shareWith, card) map {
            case Success => s!Success
            case _ => 
              Logger.warn("Share Card Failed to Add Card to Account")
              s!Failure
          }
        case DeviceNotAuthorized =>
          Logger.warn("Sharing Device is Not Authorized")
          s!Failure
        case _ =>
          Logger.warn("Share Card Received Unknown Message from Device Manager")
          s!Failure
      }
    case any =>
      Logger.warn("Unknown Message Received by Share Manager: " + any)
      sender ! Failure
  }
}

package actors

import akka.actor._
import play.api.Logger
import akka.util.Timeout
import akka.util.Duration

import com.cardtapapp.api.Main._
import controllers.Random._

// Accounts //

case class GetAccount(email: String)
case class SetAccount(account: Account)
case class Login(secret: String)
case class Authorize(code: String)
case class RedirectLogin(secret: String)
case class EnsureAccount(email: String)
case class AddCardToAccount(email: String, card: Card)

case object AccountNotAuthorized
case object AccountNotFound

class AccountManager extends Actor {

  import models.AccountsModel._
  
  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  def receive = {
    case GetAccount(email) =>
      sender ! getAccountByEmail(email)

    case AddCardToAccount(email, card) =>
      val account = {
        getAccountByEmail(email)
      }.getOrElse {
        newAccountWithEmail(email)
      }

      val cardStackOld = account.getStack()
      val cardStackNew_ = Stack.newBuilder(cardStackOld).addCards(card).build()
      val accountNew = Account.newBuilder(account).setStack(cardStackNew_).build()

      setAccountByEmail(email, accountNew)

      sender ! Success

    case EnsureAccount(email) =>
      if (!accountExists(email)) {
        newAccountWithEmail(email)
      }

    case any =>
      Logger.warn("Unknown Message Received by Account Manager: " + any)
      sender ! Failure
  }
}
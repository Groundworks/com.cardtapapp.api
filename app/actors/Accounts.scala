package actors

import akka.actor._
import play.api.Logger
import akka.util._
import akka.pattern._
import com.cardtapapp.api.Main._
import controllers.Random._
import scala.xml.persistent.SetStorage

// Accounts //

case class GetAccount(email: String)
case class SetAccount(account: Account)
case class RedirectLogin(secret: String)
case class EnsureAccountExists(email: String)
case class AddCardToAccount(email: String, card: Card)
case class UpdateStackForAccount(account:Account,stack:Stack)

case object AccountNotAuthorized
case object AccountNotFound

class AccountManager extends Actor {

  import models.AccountsModel._

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  def receive = {
    
    case UpdateStackForAccount(account:Account,stack:Stack) =>
      val account1 = Account
      	.newBuilder( getAccountByEmail(account.getEmail()).get )
      	.setStack(stack)
      	.build()
      setAccountByEmail(account.getEmail(),account1)
      sender!Success

    case GetAccount(email) =>
      sender ! getAccountByEmail(email)

    case AddCardToAccount(email, card) =>
      val s = sender
      self ? EnsureAccountExists(email) map {
        case account: Account =>
          val cardStackOld = account.getStack()
          val cardStackNew = Stack.newBuilder(cardStackOld).addCards(card).build()
          val accountNew = Account.newBuilder(account).setStack(cardStackNew).build()
          setAccountByEmail(email, accountNew)
          s ! Success
        case _ => Logger.warn("Failed to Add Card - Account Failed to Return")
      }

    case EnsureAccountExists(email) =>
      val s = sender
      if (!accountExists(email)) {
        s ! newAccountWithEmail(email)
      }else{
        self ? GetAccount(email) map { 
          case Some(account:Account) => s!account
          case _ => Logger.warn("Failed to Ensure Account Exists")
        }
      }
  }
}
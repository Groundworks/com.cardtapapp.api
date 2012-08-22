package actors

import akka.actor._
import actors.log.logger
import akka.util._
import akka.pattern._
import com.cardtapapp.api.Main._
import controllers.Random._

case class GetAccount(email: String)
case class SetAccount(account: Account)
case class RedirectLogin(secret: String)
case class EnsureAccountExists(email: String)
case class AddCardToStack(email: String, card: Card)
case class AddCardToCards(email: String, card: Card)
case class UpdateStackForAccount(account: Account, stack: Stack)

case object AccountNotAuthorized
case object AccountNotFound

class AccountManager extends Actor {

  import models.AccountsModel._

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  def getCardByIdFromStack(id: String, stack: Stack): Option[Card] = {
    if (id equals "") {
      logger.warn("Card Does Not Have UUID")
      None
    } else {
      val cards = for (
        i <- 0 until stack.getCardsCount() if (stack.getCards(i).getUuid() equals id)
      ) yield {
        logger.debug("Duplicate Card Exists")
        stack.getCards(i)
      }
      cards.headOption
    }
  }

  def receive = {

    case AddCardToCards(email: String, card: Card) =>
      val s = sender
      getAccountByEmail(email) map { account =>
        setAccountByEmail(email,
          Account
            .newBuilder(account)
            .setCards(
              Stack.newBuilder().addCards(card)).build())
        s ! Success
      } getOrElse s ! AccountNotFound

    case UpdateStackForAccount(account: Account, stack: Stack) =>
      val account1 = Account
        .newBuilder(getAccountByEmail(account.getEmail()).get)
        .setStack(stack)
        .build()
      setAccountByEmail(account.getEmail(), account1)
      sender ! Success

    case GetAccount(email) =>
      sender ! getAccountByEmail(email)

    case AddCardToStack(email, card) =>
      val s = sender
      self ? EnsureAccountExists(email) map {
        case account: Account =>
          val cardStackOld = account.getStack()
          getCardByIdFromStack(card.getUuid(), cardStackOld) getOrElse {
            val cardStackNew = Stack.newBuilder(cardStackOld).addCards(card).build()
            val accountNew = Account.newBuilder(account).setStack(cardStackNew).build()
            setAccountByEmail(email, accountNew)
          }
          s ! Success
        case _ => logger.warn("Failed to Add Card - Account Failed to Return")
      }

    case EnsureAccountExists(email) =>
      val s = sender
      if (!accountExists(email)) {
        s ! newAccountWithEmail(email)
      } else {
        self ? GetAccount(email) map {
          case Some(account: Account) => s ! account
          case _                      => logger.warn("Failed to Ensure Account Exists")
        }
      }
  }
}
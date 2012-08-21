package actors

import com.cardtapapp.api.Main._
import akka.actor._
import akka.pattern._
import play.api.Logger
import models.Database.{ connection => db }
import controllers.Random._
import akka.util.Timeout
import akka.util.Duration

// Share //

case class ShareCard(email: String, card: Card, secret: String)

class ShareManager extends Actor {

  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  val accountManager = context.actorFor("../accounts")
  val devicesManager = context.actorFor("../devices")

  def receive = {

    case ShareCard(shareWith, card, secret) =>

      val s = sender

      devicesManager ? GetDevice(secret) map {
        case auth: Authorization =>
          val access = auth.getAccess()

          Logger.debug("Access: %s" format access)
          if (access equals "VALIDATED") {
            val email = auth.getEmail()
            val stmt = db.prepareStatement("SELECT buffer FROM account WHERE email=?")
            stmt.setString(1, email)
            Logger.debug(stmt.toString())
            val rslt2 = stmt.executeQuery()

            if (rslt2.next()) {

              val share = Share.newBuilder().setCard(card).setWith(shareWith).setFrom(email).build()
              val stmt2 = db.prepareStatement("INSERT INTO share (buffer) VALUES (?)")
              stmt2.setBytes(1, share.toByteArray())
              Logger.debug(stmt2.toString())
              stmt2.executeUpdate() // possible unhandled error

              Logger.debug(stmt2.toString())
              Logger.info("New Care Shared to Email: " + shareWith)
              s ! Success

              accountManager ? AddCardToAccount(shareWith, card)

            } else {
              Logger.warn("Sharing Account Not Found")
              s ! AccountNotFound
            }
          } else {
            Logger.warn("Device Not Authorized")
            s ! DeviceNotAuthorized
          }
        case _ => s ! Failure
      }
    case any =>
      Logger.warn("Unknown Message Received by Share Manager: " + any)
      sender ! Failure
  }
}

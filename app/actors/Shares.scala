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
  
  import models.AuthorizationAccesses._
  
  implicit val timeout: Timeout = Timeout(Duration(5, "seconds"))

  val accountManager = context.actorFor("../accounts")
  
  def receive = {
    
    case ShareCard(shareWith, card, secret) =>

      // Database //
      val prep = db.prepareStatement("SELECT buffer FROM device WHERE device=?")
      prep.setString(1, secret)
      Logger.debug(prep.toString())

      val rslt = prep.executeQuery()
      if (rslt.next()) {
        val auth = Authorization.parseFrom(rslt.getBytes(1))
        val access = auth.getAccess()

        Logger.debug("Access: %s" format access)
        if (access equals AUTH_VALIDATED) {
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
            sender ! Success

            accountManager ? AddCardToAccount(shareWith, card)

          } else {
            Logger.warn("Sharing Account Not Found")
            sender ! AccountNotFound
          }
        } else {
          Logger.warn("Device Not Authorized")
          sender ! DeviceNotAuthorized
        }
      } else {
        Logger.warn("Device Not Registered")
        sender ! DeviceNotRegistered
      }
    case any =>
      Logger.warn("Unknown Message Received by Share Manager: " + any)
      sender ! Failure
  }
}

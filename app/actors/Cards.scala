package actors

import com.cardtapapp.api.Main._
import akka.actor._
import play.api.Logger
import models.Database.{ connection => db }
import controllers.Random._
import akka.util.Timeout
import akka.util.Duration

// Cards //

case class AddCard(face: String, rear: String)

class CardManager extends Actor {
  def receive = {
    case AddCard(face, rear) =>
      {
        val _uuid = uuid
        val card = Card.newBuilder()
          .setUuid(_uuid)
          .setImageFace(face)
          .setImageRear(rear)
          .build()
        val stmt = db.prepareStatement("INSERT INTO card (uuid,buffer) VALUES (?,?)")
        stmt.setString(1, _uuid)
        stmt.setBytes(2,
          card.toByteArray())
        val rslt = stmt.executeUpdate()
        if (rslt > 0) {
          Logger.info("New Card Inserted at %s" format _uuid)
          sender ! card
        } else {
          sender ! Failure
        }
      }
  }
}
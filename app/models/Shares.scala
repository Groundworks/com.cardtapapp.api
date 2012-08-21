package models

import controllers.Random._
import com.cardtapapp.api.Main._
import models.Database.{ connection => db }
import models.log.logger

object SharesModel {
  def newShare(share: Share) {
    val stmt2 = db.prepareStatement("INSERT INTO share (buffer) VALUES (?)")
    stmt2.setBytes(1, share.toByteArray())
    logger.debug(stmt2.toString())
    stmt2.executeUpdate() // possible unhandled error
  }
}
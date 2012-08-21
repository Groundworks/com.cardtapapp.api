package models

import controllers.Random._
import com.cardtapapp.api.Main._
import models.Database.{ connection => db }
import play.api.Logger

object AuthorizationAccesses {
  val AUTH_VALIDATED = "VALIDATED"
  val AUTH_PENDING = "PENDING"
}

object DevicesModel {

  def secret = uuid.substring(0, 4)

  def device = Device
    .newBuilder()
    .setUuid(uuid)
    .setSecret(secret)
    .build()

  def authorization(email: String) = Authorization
    .newBuilder()
    .setUuid(uuid)
    .setCode(secret)
    .setDevice(device)
    .setEmail(email)
    .build()

  def getAuthorizationFromSecret(secret: String) = {
    val stmt = db.prepareStatement("SELECT buffer FROM device WHERE device=?")
    stmt.setString(1, secret)
    val rslt = stmt.executeQuery()
    if (rslt.next()) {
      Some(Authorization.parseFrom(rslt.getBytes(1)))
    } else {
      None
    }
  }

  def newAuthorizationFromEmail(email: String) = {
    val auth = authorization(email)
    val prep = db.prepareStatement("INSERT INTO device (authcode,device,buffer) VALUES (?,?,?)")
    prep.setString(1, auth.getCode())
    prep.setString(2, auth.getDevice().getSecret())
    prep.setBytes(3, auth.toByteArray())
    Logger.debug(prep.toString())
    prep.executeUpdate() // Unchecked
    auth
  }
}
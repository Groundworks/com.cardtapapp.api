package models

import util.Random._
import com.cardtapapp.api.Main._
import models.Database.{ connection => db }
import models.log.logger

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

  def setAuthorizationFromCode(code: String, auth: Authorization) {
    val stmt = db.prepareStatement("UPDATE device SET buffer=? WHERE authcode=?")
    stmt.setBytes(1, auth.toByteArray())
    stmt.setString(2, code)
    logger.debug(stmt.toString())
    val rtn = stmt.executeUpdate()
  }

  def getAuthorizationFromCode(code: String) = {
    val stmt = db.prepareStatement("SELECT buffer FROM device WHERE authcode=?")
    stmt.setString(1, code)
    logger.debug(stmt.toString)
    val rslt = stmt.executeQuery()

    // Results //
    if (rslt.next()) {

      val buffer = rslt.getBytes(1)
      val authzn = Authorization.parseFrom(buffer)

      Some(authzn)
    } else {
      None
    }
  }

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
    logger.debug(prep.toString())
    prep.executeUpdate() // Unchecked
    auth
  }
}
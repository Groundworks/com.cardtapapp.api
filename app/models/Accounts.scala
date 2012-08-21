package models

import play.api.Logger
import models.Database.{ connection => db }
import com.cardtapapp.api.Main._
import controllers.Random._

object AccountsModel {
  
  def accountExists(email: String): Boolean = {
    val stmt = db.prepareStatement("SELECT COUNT(id) FROM account WHERE email=?")
    stmt.setString(1, email)
    Logger.debug(stmt.toString())
    val rslt = stmt.executeQuery()
    rslt.next()
    rslt.getInt(1) > 0
  }

  def newAccountWithEmail(email: String): Account = {
    val act = Account.newBuilder().setEmail(email).setUuid(uuid).build()
    val stmt = db.prepareStatement("INSERT INTO account (buffer,email) VALUES (?,?)")
    stmt.setBytes(1, act.toByteArray())
    stmt.setString(2, email)
    Logger.debug(stmt.toString)
    stmt.executeUpdate() // Unchecked
    act
  }

  def getAccountByEmail(email: String): Option[Account] = {
    val stmt = db.prepareStatement("SELECT buffer FROM account WHERE email=?")
    stmt.setString(1, email)
    Logger.debug(stmt.toString)
    val rslt = stmt.executeQuery()
    if (rslt.next()) {
      val buffer = rslt.getBytes(1)
      val account = Account.parseFrom(buffer)
      Some(account)
    } else {
      Logger.warn("Cannot Find Account (%s) in Add Card to Account" format email)
      None
    }
  }

  def setAccountByEmail(email: String, account: Account) {
    val stmt = db.prepareStatement("UPDATE account SET buffer=? WHERE email=?")
    stmt.setBytes(1, account.toByteArray())
    stmt.setString(2, email)
    Logger.debug(stmt.toString)
    stmt.executeUpdate() // Unchecked
  }
}
package controllers

import anorm._

import com.dd.plist._

import com.cardtapapp.api.Main._
import org.scalaquery.session.{ Session => DBSession, Database }
import org.scalaquery.ql.basic._
import org.scalaquery.ql.basic.BasicDriver.Implicit._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql._

import play.api._
import play.api.mvc._

import java.util.UUID
import play.api.db.DB
import play.api.Play.current

object Devices extends BasicTable[(String, Array[Byte])]("device") {
  def id = column[Int]("id", O PrimaryKey)
  def secret = column[String]("secret", O Default "NFN", O DBType "TEXT")
  def buffer = column[Array[Byte]]("buffer")
  def * = secret ~ buffer
}

object DataStore {

  val db = Database.forURL("jdbc:postgresql://localhost/demo", driver = "org.postgresql.Driver")

  def setDevice(device: Device) {
    db withSession { implicit session: DBSession =>
      Devices insert (device.getSecret(), device.toByteArray())
    }
  }
  
  def getDeviceBySecret(secret: String): Option[Device] = {
    val res = for (d <- Devices if d.secret === secret) yield d.buffer
    db withSession { implicit session: DBSession =>
      res.firstOption().map { bytes => 
        Device.parseFrom(bytes)
      }
    }
  }
}

object Application extends Controller {

  def uuid = UUID.randomUUID().toString()

  def register = Action { request =>
    request.body.asFormUrlEncoded.map { form =>
      form.get("email").map { email =>
        val device = Device.newBuilder()
          .setAccountid(email.head)
          .setSecret(uuid)
          .setUuid(uuid)
          .setAuthorized(false)
          .build()
        DataStore.setDevice(device)
        val redirect = "/login/" + device.getSecret()
        Created(device.getUuid()).withHeaders("Location" -> redirect)
      }.getOrElse { BadRequest }
    }.getOrElse { BadRequest }
  }

  def login(device: String) = Action {
    DataStore.getDeviceBySecret(device).map { device =>
      Ok
    }.getOrElse { InternalServerError }
  }

}

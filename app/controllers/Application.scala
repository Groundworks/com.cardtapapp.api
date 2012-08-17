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

object Devices extends BasicTable[(String, String, Array[Byte])]("device") {
  def id = column[Int]("id", O PrimaryKey, O DBType "Integer")
  def device = column[String]("device", O Default "NFN", O DBType "TEXT")
  def secret = column[String]("secret", O Default "NFN", O DBType "TEXT")
  def buffer = column[Array[Byte]]("buffer")
  def * = device ~ secret ~ buffer
}

object Mailer {
  def notifyOfDevice(email: String, device: Device) {
    Logger.info("Email: %s with Confirmation Code: %s" format (email, device.getSecret()))
  }
}

object DataStore {

  val db = Database.forURL("jdbc:postgresql://localhost/demo", driver = "org.postgresql.Driver")

  def setDevice(device: Device) {
    db withSession { implicit session: DBSession =>
      val q = for (d <- Devices if d.device === device.getUuid()) yield d.buffer
      q.update(device.toByteArray)
    }
  }

  def newDevice(device: Device) {
    db withSession { implicit session: DBSession =>
      Devices insert (device.getUuid(), device.getSecret(), device.toByteArray())
    }
  }

  def getDeviceByUuid(uuid: String): Option[Device] = {
    val res = for (d <- Devices if d.device === uuid) yield d.buffer
    db withSession { implicit session: DBSession =>
      res.firstOption().map { bytes =>
        Device.parseFrom(bytes)
      }
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
      form.get("email").map { emailseq =>
        val email = emailseq.head
        val device = Device.newBuilder()
          .setAccountid(email)
          .setSecret(uuid)
          .setUuid(uuid)
          .setAuthorized(false)
          .build()
        DataStore.newDevice(device)
        Mailer.notifyOfDevice(email, device)
        val redirect = "/login/" + device.getUuid()
        Created(device.getUuid()).withHeaders("Location" -> redirect)
      }.getOrElse { BadRequest }
    }.getOrElse { BadRequest }
  }

  def login(uuid: String) = Action {
    DataStore.getDeviceByUuid(uuid).map { device =>
      if (device.getAuthorized()) {
        Ok
      } else {
        Unauthorized
      }
    }.getOrElse { NotFound }
  }

  def authorize(secret: String) = Action {
    DataStore.getDeviceBySecret(secret).map { device =>
      if (device.getAuthorized()) {
        NotFound
      } else {
        val authdev = Device.newBuilder(device).setAuthorized(true).build()
        DataStore.setDevice(authdev)
        Created
      }
    }.getOrElse{NotFound}
  }
}

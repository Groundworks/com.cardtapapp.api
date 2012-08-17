package controllers

import anorm._

import com.dd.plist._

import com.cardtapapp.api.Main._

import play.api._
import play.api.mvc._

import java.util.UUID
import play.api.db.DB
import play.api.Play.current

object DataStore {

  def setDevice(device: Device) {
    DB.withConnection { implicit c =>
      SQL("INSERT INTO device (buffer,secret) VALUES ({secret},{buffer})").on(
        "buffer" -> device.toByteArray(),
        "secret" -> device.getSecret())
    }
  }
  
  def getDeviceBySecret(secret: String): Option[Device] = {
    DB.withConnection { implicit c =>
      SQL("SELECT buffer FROM device WHERE secret={secret}").on(
        "secret" -> secret).apply().head match {
          case Row(bytes: Array[Byte]) => Some(Device.parseFrom(bytes))
          case _                       => None
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
        Created(device.getUuid()).withHeaders("Location" -> "/login/")
      }.getOrElse { BadRequest }
    }.getOrElse { BadRequest }
  }

  def login(device: String) = Action {
    Ok
  }

}

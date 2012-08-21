package models

import java.sql._
import play.api.Play.current
import play.api.db.DB

object Database {  
  lazy val connection: Connection = DB.getConnection()
}

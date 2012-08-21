package models

import java.sql._

object Database {  
  val connectionString = "jdbc:postgresql://localhost/demo"
  lazy val connection: Connection = DriverManager.getConnection(connectionString)
}

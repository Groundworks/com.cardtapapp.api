package models

import java.sql._

object Database {
  
  val connectionString = "jdbc:postgresql://localhost/demo"
  val connection: Connection = DriverManager.getConnection(connectionString)

}

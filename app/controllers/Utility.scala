package controllers

import java.util.{ UUID => JavaUUID }

object Random {
  def uuid(): String = {
    JavaUUID.randomUUID().toString
  }
}

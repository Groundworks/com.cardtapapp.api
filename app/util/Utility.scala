package util

import java.util.{ UUID => JavaUUID }
import play.api.Play
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Random {
  def uuid(): String = {
    JavaUUID.randomUUID().toString
  }
}

object HMac {
  val secret = Play.current.configuration.getString("application.secret").get
  
  val mac = Mac.getInstance("HmacSHA256")
  val key = new SecretKeySpec(secret.getBytes,"HmacSHA256")
  
  def sign(bytes:Array[Byte]): String = {
    mac.init(key)
    mac.doFinal(bytes).map{
      b:Byte => "%02x" format b 
    }.foldLeft("")((hex,str)=>str+hex)
   
  }
  
}
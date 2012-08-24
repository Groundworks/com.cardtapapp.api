package util

import java.util.{ UUID => JavaUUID }
import play.api.Play
import messages.Main._
import java.security.spec.KeySpec
import javax.crypto.spec._
import javax.crypto._
import java.security.SecureRandom
import java.math.BigInteger
import sun.misc.BASE64Encoder
import com.google.protobuf.ByteString

object Random {
  def uuid(): String = {
    JavaUUID.randomUUID().toString
  }
}

object HMac {
  val secret = Play.current.configuration.getString("application.secret").get

  val mac = Mac.getInstance("HmacSHA256")
  val key = new SecretKeySpec(secret.getBytes, "HmacSHA256")

  def sign(bytes: Array[Byte]): String = {
    mac.init(key)
    mac.doFinal(bytes).map {
      b: Byte => "%02x" format b
    }.foldLeft("")((hex, str) => str + hex)

  }

}

object AuthHeaders {
    
  implicit def byteArrayToByteString(bytes: Array[Byte]): ByteString = {
    ByteString.copyFrom(bytes)
  }

  def authHeader(id: String, secret: String) = {
    val cred = Credentials.newBuilder().setDevice(id).setSecret(secret).build.toByteArray()
    val auth = new BASE64Encoder().encode(cred)
    Map("Authorization" -> Seq(auth))
  }
}

object PBKDF2 {

  val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
  val random = new SecureRandom()

  def pbhash(value: String) = {
    val salt = new BigInteger(1024, random) // random 130 bit integer 
    val ks: KeySpec = new PBEKeySpec(value.toCharArray(), salt.toByteArray(), 1024, 128)
    val secretKey = factory.generateSecret(ks)
    (secretKey.getEncoded(), salt.toByteArray())
  }

  def pbhash(value: String, salt: Array[Byte]) = {
    val ks: KeySpec = new PBEKeySpec(value.toCharArray(), salt, 1024, 128)
    val secretKey = factory.generateSecret(ks)
    secretKey.getEncoded()
  }

}
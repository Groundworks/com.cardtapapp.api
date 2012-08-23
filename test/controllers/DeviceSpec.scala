package controllers

import messages.Main._
import org.scalatest._
import play.api.test._
import play.api.mvc._
import play.api.test.Helpers._
import org.scalatest.matchers.MustMatchers._

import java.security.spec.KeySpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto._
import java.security.SecureRandom
import java.math.BigInteger
import com.google.protobuf.ByteString

class DeviceSpec extends FeatureSpec with GivenWhenThen {

  implicit def byteArrayToByteString(bytes: Array[Byte]): ByteString = {
    ByteString.copyFrom(bytes)
  }

  import util.PBKDF2._
  
  feature("Use Password Hashes with Salts") {

    scenario("Device authenticates by password hash") {

      val email = "bob@example.com"
      val uuid = java.util.UUID.randomUUID().toString()
      val devid = "asdf"
      val dev = Device.newBuilder().setAccount(email).build()
      val pass = "This is my password"
      val (hash, salt) = pbhash(pass)
      val auth = Authorization.newBuilder().setDevice(dev).setSecretHash(hash).setSecretSalt(salt).build()
      val acct = Account.newBuilder().setEmail(email).build()
      
      object A extends App {
        val accounts = collection.mutable.Map[String, Account](uuid -> acct)
        val authorizations = collection.mutable.Map[String, Authorization](devid -> auth)
      }
      
      val request = new FakeRequest(
        "GET",
        "http://localhost/device/" + devid,
        new FakeHeaders(Map("Authorization" -> Seq("pass"))),
        new play.api.mvc.AnyContentAsText(""))
      val resp = A.deviceGet(devid)(request)
      status(resp) must equal(200)

      val bytes = contentAsBytes(resp)
      val reply_dev = Device.parseFrom(bytes)

      reply_dev.getAccount() must equal(email)

    }
  }

  feature("Device can retreive its device protobuf") {

    scenario("Device with not authentication cannot retrieve any data") {

      object A extends App {
        val accounts = collection.mutable.Map[String, Account]()
        val authorizations = collection.mutable.Map[String, Authorization]()
      }
      val id = "abc123"
      when("User Gets a device id without any authorization header")
      val request = new FakeRequest(
        "GET",
        "http://localhost/device/" + id,
        new FakeHeaders(),
        new play.api.mvc.AnyContentAsText(""))
      val resp = A.deviceGet(id)(request)
      status(resp) must equal(UNAUTHORIZED)
      then("Return unautorized")

    }

    scenario("Device with incorrect authentication is not granted access") {
      object A extends App {
        val accounts = collection.mutable.Map[String, Account]()
        val authorizations = collection.mutable.Map[String, Authorization]()
      }
      val id = "abc123"
      when("User Gets a device id with improper authorization header")
      val request = new FakeRequest(
        "GET",
        "http://localhost/device/" + id,
        new FakeHeaders(Map("Authorization" -> Seq("Tom"))),
        new play.api.mvc.AnyContentAsText(""))
      val resp = A.deviceGet(id)(request)
      status(resp) must equal(UNAUTHORIZED)
      then("Return unauthorized")
    }
  }

}
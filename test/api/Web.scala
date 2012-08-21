package api

import dispatch._
import com.cardtapapp.api.Main._

object Web {

  val host = "http://localhost:9000"
  
  def post(path: String)(query: (String, String)*) = {
    val svc = url(host + path).POST
    for (q <- query) {
      val (key, value) = q
      svc.addQueryParameter(key, value)
    }
    Http(svc)()
  }

  def get(path: String) = {
    Http(url(host + path))()
  }

  def register(email: String) = {
    val rsp = post("/register")("email" -> email)
    val login = rsp.getHeader("Location")
    rsp.getResponseBody()
  }

  def poll(secret: String) = {
    val rsp = get("/poll/" + secret)
    rsp.getResponseBody()
  }

  def login(secret: String) = {
    get("/login/" + secret)
  }

  def auth(code: String) = {
    get("/auth/" + code)
  }

  def device(email: String) = {
    val secret = register(email)
    auth(poll(secret))
    secret
  }

  def account(secret: String) = {
    Account.parseFrom(login(secret).getResponseBodyAsBytes())
  }

}
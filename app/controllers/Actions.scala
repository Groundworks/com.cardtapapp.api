package controllers

import play.api.mvc._
import messages.Main.AccessToken
import com.google.protobuf.Message

trait DecodeAccessToken[A] extends Action[A]
object DecodeAccessToken {
  def base64decode(code: String) = new sun.misc.BASE64Decoder().decodeBuffer(code)
  def apply[A](bodyParser: BodyParser[A])(block: AccessToken => Request[A] => Result) = new DecodeAccessToken[A] {
    def parser = bodyParser
    def apply(req: Request[A]) = {
      req.headers.get("Authentication").map { auth =>
        val bytes = base64decode(auth)
        val token = AccessToken.parseFrom(bytes)
        block(token)(req)
      } getOrElse Results.Unauthorized
    }
  }
  def apply(block: AccessToken => Request[AnyContent] => Result): Action[AnyContent] = {
    DecodeAccessToken(BodyParsers.parse.anyContent)(block)
  }
}

trait DecodeProtobuf[P <: Message, A] extends Action[A]
object DecodeProtobuf {
  def base64decode(code: String) = new sun.misc.BASE64Decoder().decodeBuffer(code)
  def apply[P <: Message, A](bodyParser: BodyParser[A], proto: Class[P])(block: P => Request[A] => Result) = new DecodeProtobuf[P, A] {
    def parser = bodyParser
    def apply(req: Request[A]) = {
      req.body.asInstanceOf[AnyContent].asRaw.flatMap { raw =>
        raw.asBytes().map { bytes =>
          try {
            val parseFrom = proto.getMethod("parseFrom", classOf[Array[Byte]])
            val message: P = parseFrom.invoke(proto, bytes).asInstanceOf[P]
            block(message)(req)
          }catch{
            case e:NoSuchMethodException => Results.BadRequest
          }
        }
      }
    } getOrElse { Results.InternalServerError }
  }
  def apply[P <: Message](proto: Class[P])(block: P => Request[AnyContent] => Result): Action[AnyContent] = {
    DecodeProtobuf(BodyParsers.parse.anyContent, proto)(block)
  }
}

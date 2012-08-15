package controllers

import play.api._
import play.api.mvc._

import com.dd.plist._

import java.util.UUID

case class Registration(
  email  : String,
  device : String,
  authcode : String,
  var authorized : Boolean
)

object Application extends Controller {

  // Multiple Indexes for the Same Data //
  val registration  = collection.mutable.Map[String,Registration]()
  val authorization = collection.mutable.Map[String,Registration]()

  def sendEmailAuthorization(email:String,code:String){
    Logger.info("Sending Authorization Code %s" format code)
  }
  
  def register = Action { implicit request =>
    request.body.asFormUrlEncoded.map { form =>
      form.get("email").map { emails =>
        
        val email = emails.head
        val uuid = UUID.randomUUID().toString()
        val auth = UUID.randomUUID().toString()
        
        val reg = Registration(email,uuid,auth,false)
        
        registration(uuid)  = reg
        authorization(auth) = reg
        
        sendEmailAuthorization(email,auth)
        
        Logger.info("User %s Registered Device %s with Authorization Code %s" format (email,uuid,auth))
        
        Created.withHeaders("Location"-> routes.Application.login(uuid).toString )
      }.getOrElse{BadRequest("Expecting Email in Form Data")}
    }.getOrElse{
      BadRequest("Expecting Form Data")
    }
  }
  
  def authorize(authcode:String) = Action {
    Logger.info("Authorizing with Code %s" format authcode)
    authorization.get(authcode).map { registration => 
      registration.authorized = true
      Created.withHeaders("Location"->routes.Application.login(registration.device).toString )
    }.getOrElse{NotFound}
  }

  def login(device:String) = Action{
    Logger.info("Attempting Device %s Log In" format device)
    registration.get(device).map { registration =>
      if(registration.authorized){
        Logger.info("Device %s Login Success" format device)
        Ok
      }else{
        Logger.info("Device %s Not Authorized" format device)
        Unauthorized("This Device Has Not Been Authorized")
      }
    }.getOrElse{NotFound}
  }

  def dump = Action {
    val regs = registration.foldLeft("\n")((out,next)=>out+next+"\n") + 
               authorization.foldLeft("\n")((out,next)=>out+next+"\n")
    Ok(regs)
  }
  
}

package controllers

import play.api._
import play.api.mvc._

import com.dd.plist._

object Application extends Controller {
  
  def user(userkey:String) = Action { request =>
    
    Logger.info("Get User Account: %s" format userkey)
    Logger.debug(request.headers.toSimpleMap.foldLeft("Headers:\n"){case (x,(y,z))=> x+y+":"+z+"\n" })
    
    if(userkey==uid){
      Ok(views.html.user()).withHeaders("Content-Type"->"application/plist")
    }else{
      NotFound("")
    }
  }
  
  val uid = "437"
  val key = "e29"
  val dev = "85e"
  
  def poll(devkey:String) = Action {
    Logger.info("Polling for Device: %s" format devkey)
    if(confirmed){
      if(devkey==dev){
        confirmed=false
        Redirect(routes.Application.user(uid))
      }else{
        NotFound("")
      }
    }else{
      Forbidden("Please Confirm Device by Email")
    }
  }
  
  def register(user:String) = Action { request =>
    val device = dev
    
    Logger.info("Register New Device: %s to User: %s" format (device,user))
    Logger.info("To Confirm Please Follow the URL: http://%s%s" format (request.headers("Host"),routes.Application.confirm(key)))
    
    Redirect(routes.Application.poll(dev))
  }
  
  var confirmed = false
  def confirm(confirmKey:String) = Action { request=>
    if(confirmKey==key){
      confirmed=true
      Logger.info("Confirmed Key: %s" format confirmKey)
      Redirect("cardtapapp://"+request.headers("Host")+"/poll/" + dev)
    }else{
      NotFound("")
    }
  }
  
  def index = Action {
    Redirect("http://cardtapapp.com")
  }
  
}

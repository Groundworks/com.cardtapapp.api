package controllers

import play.api._
import play.api.mvc._

import com.dd.plist._

object Application extends Controller {
  
  def user(userid:String) = Action {
    Logger.info("Get User Account: %s" format userid)
    val nsdict = new NSDictionary()
    
    val byteArray = BinaryPropertyListWriter.writeToArray(nsdict)
    Ok(byteArray).withHeaders("Content-Type"->"application/plist")
  }
  
  def share(from:String,to:String) = Action {
    Logger.info("Share from: %s to: %s" format (from,to))
    TODO
  }
  
  def register(device:String,user:String) = Action {
    val deviceId = "None"
    Logger.info("Register New Device: %s to User: %s" format (deviceId,user))
    TODO
  }
  
  def index = Action {
    Redirect("http://cardtapapp.com")
  }
  
}
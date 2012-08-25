package controllers

import protobuf.Main._
import play.api.mvc._
import controllers._
import controllers.Implicits._
import models._

// -- Controllers -- //

// POST //
object Post extends Controller {

  def register = DecodeProtobuf(classOf[Registration]) { registration =>
    Action {
      val token = Repository.getClientById(Repository.newClientWithEmail(registration.getEmail())).getToken()
      Created(token)
    }
  }

  def share(email: String) = DecodeProtobuf(classOf[Index]) { index =>
    DecodeAccessToken { clientid =>
      Action {
        Repository.postToInbox(clientid, index)
        Accepted
      }
    }
  }
}

// GET //
object Get extends Controller {

  def confirm(key: String) = Action {
    Redirect("/stack")
  }

  def stack(uuid: String) = DecodeAccessToken { clientid =>
    Action {
      Ok(Repository.getStack(clientid))
    }
  }
  
  def card(cardid: String) = DecodeAccessToken { token =>
    Action {
      Repository.getCard(cardid).map { card =>
        Ok(card)
      } getOrElse {
        NotFound
      }
    }
  }
  
  def inbox = DecodeAccessToken { clientid =>
    Action {
      val stack = Repository.getInbox(clientid)
      Ok(stack)
    }
  }
  
}

// PUT //
object Put extends Controller {
  def stack(uuid: String) = DecodeAccessToken { clientid =>
    DecodeProtobuf(classOf[Stack]) { stack =>
      Repository.putStack(clientid, stack)
      Action {
        NoContent
      }
    }
  }
}


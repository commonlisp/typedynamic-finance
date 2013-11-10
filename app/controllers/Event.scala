package controllers

import play.api._
import play.api.mvc._

object Event extends Controller {
	
def newEvent() = Action { implicit req =>
  Ok("newEvent")

}

def show(id : String) = Action {
  Ok("show " + id)
}

}
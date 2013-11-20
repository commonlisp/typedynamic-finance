package controllers

import play.api._
import play.api.mvc._

import com.mongodb.casbah.Imports._

object Event extends Controller {

  val coll = MongoConnection()("typedynamic")("events")	

  def newEvent() = Action { implicit req =>
    Ok("newEvent")
  }

  def show(id : String) = Action {
    val o = MongoDBObject("id" -> id)
    coll.findOne(o) match {
      case None => Ok("Not found")
      case Some(m) => Ok("Found " + m)
    }
    Ok("Error")
  }

}

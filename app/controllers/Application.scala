package controllers

import play.api._
import play.api.mvc._
import org.jsoup.Jsoup

import collection.immutable._

import play.api.db.DB
import anorm._
import play.api.Play.current

import models.BondPortfolio

case class ETF(issuer: String, symbol: String, duration: Float, bondYield: Float)

object Application extends Controller {

  val issuerQuery = HashMap("ishares" -> ((x : String) => "http://us.ishares.com/product_info/fund/overview" + x + ".htm")) 

  def etfYield(symbol : String): Float = {
    return Jsoup.connect("http://finance.yahoo.com/q?s=" + symbol).get.select("yield").text.toFloat
  }
 
  def index = Action { DB.withConnection { implicit c =>
    val yield10 = 
        Jsoup.connect("http://finance.yahoo.com/q?s=^tnx").get.select(".time_rtq_ticker").first.text
   
    val selectDurations = SQL("select symbol, duration from bondetf")
    Ok(views.html.index(HashMap("10 Year" -> yield10.toString.toFloat, 
                                "High Yield" -> etfYield("JNK"), 
                                "Investment Grade Corporates" -> etfYield("LQD"), 
                                "Emerging Govies" -> etfYield("ELD"),
                                "MBS" -> etfYield("MBB")
                                )))
  }
  }
  
}

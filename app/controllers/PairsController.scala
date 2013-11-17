package controllers

import play.api._
import play.api.mvc._
import org.jsoup.Jsoup

import collection.immutable._

import java.net.{URL, URLConnection, URLEncoder}

import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import models._

import play.api.data._
import play.api.data.Forms._

import collection.JavaConversions._

import breeze.linalg._

import com.mongodb.casbah.Imports._

import io.Source

object PairsController extends Controller {

  val coll = MongoConnection()("typedynamic")("pairs")

  def periodPrices(symbol: String, days: Int): List[Double] = {
     return Source.fromInputStream(new URL("http://ichart.finance.yahoo.com/table.csv?s="+symbol+"&d=6&e=22&f=2012&g=d&a=6&b=9&c=2011&ignore=.csv").
            openConnection.getInputStream).getLines().
            map(line => line.split(",")(6)).drop(1).take(days).
            map(p => p.toDouble).toList
  }
  
  def periodReturn(prices: List[Double]): Double = {
    return (prices.head - prices.last) / prices.last
  }

  def periodPairReturn(long: String, short: String, days: Int): Double = {
    val longPrices = periodPrices(long, days)
    val shortPrices = periodPrices(short, days)
    return periodReturn(longPrices) - periodReturn(shortPrices)
  }


  def maxReturnPair(prices: DenseMatrix[Double]): (Int, Int) = {
    return (0, 0)
  }
  

  def portfolioComponents(symbol: String): List[String] = {
    val queryURLstr = "http://finance.yahoo.com/q/cp?s="
    return Jsoup.connect(queryURLstr + URLEncoder.encode(symbol,"UTF-8")).get.
           select(".yfnc_tabledata1 a").map(x => x.text()).toList
  }

  def portfolioReturns(symbol: String, days: Int): DenseMatrix[Double] = {
    val components = portfolioComponents(symbol)
    return new DenseMatrix[Double](days, 
                           components.size(), 
                           components.flatMap(c => periodPrices(c, days)).toArray)
  }

  def show(long: String, short: String) = Action {
     val ret = periodPairReturn(long, short, 5)
     val o = MongoDBObject("pair" -> MongoDBObject("long" -> long, "short" -> short, "ret" -> ret))
     coll.findOne(o) match {
        case None => coll += o
        case Some(_) => ()
     }
     Ok(views.html.pairs.show(
           Map("pairReturn" -> ret.toString)))
  } 

  def index() = Action {
    val results = coll.map(x => x.getAs[DBObject]("pair").
                                 map( y => (y("long").asInstanceOf[String], 
                                            y("short").asInstanceOf[String] ))).
                       map(_.get).toList
    println(results)
    Ok(views.html.pairs.index(results))
  }
}


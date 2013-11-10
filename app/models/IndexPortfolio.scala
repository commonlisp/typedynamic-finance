package models

import collection.JavaConversions._
import org.jsoup.Jsoup

import com.mongodb.casbah.Imports._

object IndexPortfolio {

   val indexholdings = MongoConnection()("typedynamic")("indexholdings")

   def apply(symbol: String): List[String] = {
      val query = MongoDBObject("symbol" -> symbol)
      indexholdings.findOne(query) match {
        case Some(obj) => return obj.as[List[String]]("holdings")
        case None => {
	  val infodoc = Jsoup.
               connect("http://us.ishares.com/product_info/fund/holdings/" + 
                       symbol + ".htm").get
          val holdings = infodoc.select("#holdings-eq #holdings-table-body tr #holding-ticker").map(_.text).toList
	  indexholdings += MongoDBObject("symbol" -> symbol, "holdings" -> holdings)
          return holdings
        }
      }
   }
}

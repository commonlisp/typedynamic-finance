package controllers

import play.api._
import play.api.mvc._
import org.jsoup.Jsoup

import collection.immutable._

import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import models._

import play.api.data._
import play.api.data.Forms._

import collection.JavaConversions._

object BondETF extends Controller {
   val bondportfolio = {
     get[Long]("id") ~
     get[String]("symbol") ~
     get[String]("descr") map { 
      case id~symbol~descr => BondPortfolio(id, symbol, descr, 1.0f)
     }
   }

   def all(): List[BondPortfolio] = DB.withConnection { implicit c => 
      SQL("select * from bondportfolio").as(bondportfolio *)
   }

   def show(symbol: String) = Action {
          
     Ok("Bond ETF " + symbol + all())
   }
  
   val bondPortfolioForm = Form(
      tuple(
        "symbol" -> nonEmptyText,
        "descr" -> nonEmptyText
      )
   )
 
   def bondportfolios() = Action {
      val portfolios = all()
      val rates = portfolios.map(p => (p.symbol, Application.etfYield(p.symbol))).toMap
      Ok(views.html.bondportfolios(portfolios, rates, bondPortfolioForm))
   }

   def newBondPortfolio() = Action { implicit req =>
     bondPortfolioForm.bindFromRequest.fold(
       errors => BadRequest(views.html.bondportfolios(all(), Map.empty, errors)),
       x => {
             BondPortfolio.create(x._1, x._2, 4.0f)
             Redirect(routes.BondETF.bondportfolios)
       })
   }
}

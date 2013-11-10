package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current


case class BondPortfolio(id: Long, symbol: String, descr: String, duration: Float)

object BondPortfolio {
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
  def create(symbol: String, descr: String, duration: Float) {
     DB.withConnection { implicit c => 
       SQL("insert into bondportfolio (symbol, descr, duration) values ({symbol},{descr}, {duration})").on('symbol -> symbol, 'descr -> descr, 'duration -> duration).executeUpdate() 
     }
  }
  def delete(id: Long) {}
}

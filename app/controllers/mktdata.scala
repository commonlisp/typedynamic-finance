package controllers

import models._

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._

import java.util.{Date, Calendar}

import com.mongodb.casbah.Imports._
import com.ib.client._

import collection.mutable._

import play.api.libs.concurrent.Execution.Implicits.defaultContext

object MktData extends Controller {

  var id = 0
  val cl = new EClientSocket(IBWrapper)  

  val coll = MongoConnection()("typedynamic")("mktdataSubscriptions")

  object IBWrapper extends EWrapper 
  {

    def tickSnapshotEnd(x: Int) {} 
    def deltaNeutralValidation(x: Int, x0: com.ib.client.UnderComp) {}
    def fundamentalData(x: Int, x0: java.lang.String) {}
    def currentTime(x: Long) {}
    def realtimeBar(reqId: Int, time: Long, open: Double, high: Double, low: Double, close: Double, volume: Long, wap: Double, count: Int) {}
    def scannerDataEnd(x: Int) {}
    def scannerData(x: Int, x0: Int, x1: com.ib.client.ContractDetails, x2: String, x3: String, x4: java.lang.String, x5: java.lang.String) {}
    def scannerParameters(x: java.lang.String) {}
    def historicalData(x1: Int, x2: java.lang.String, x3: Double, x4: Double, x5: Double, x6: Double, x7: Int, x8: Int, x9: Double, x10: Boolean) {}
    def receiveFA(x1: Int, x2: java.lang.String) {}
    def managedAccounts(x1: java.lang.String) {}
    def updateNewsBulletin(x1: Int, x2: Int, x3: java.lang.String, x4: java.lang.String) {}
  def updateMktDepthL2  (x1: Int, x2: Int, x3: java.lang.String, x4: Int, x5: Int, x6: Double, x7: Int) {}
  def updateMktDepth  (x1: Int, x2: Int, x3: Int, x4: Int, x5: Double, x6: Int) {}
  def execDetailsEnd  (x1: Int) {}
  def execDetails  (x1: Int, x2: com.ib.client.Contract, x3: com.ib.client.Execution) {}
  def contractDetailsEnd  (x1: Int) {}
  def bondContractDetails  (x1: Int, x2: com.ib.client.ContractDetails) {}
  def contractDetails  (x1: Int, x2: com.ib.client.ContractDetails) {}
  def nextValidId  (x1: Int) {}
  def accountDownloadEnd  (x1: java.lang.String) {}
  def updateAccountTime  (x1: java.lang.String) {}
  def updatePortfolio  (x1: com.ib.client.Contract, x2: Int, x3: Double, x4: Double, x5: Double, x6: Double, x7: Double, x8: java.lang.String) {}
  def updateAccountValue  (x1: java.lang.String, x2: java.lang.String, x3: java.lang.String, x4: java.lang.String) {}
  def openOrderEnd  () {}
  def openOrder  (x1: Int, x2: com.ib.client.Contract, x3: com.ib.client.Order, x4: com.ib.client.OrderState) {}
  def orderStatus  (x1: Int, x2: java.lang.String, x3: Int, x4: Int, x5: Double, x6: Int, x7: Int, x8: Double, x9: Int, x10: java.lang.String) {}
  def tickEFP  (x1: Int, x2: Int, x3: Double, x4: java.lang.String, x5: Double, x6: Int, x7: java.lang.String, x8: Double, x9: Double) {}
  def tickString  (x1: Int, x2: Int, x3: java.lang.String) {}
  def tickGeneric  (x1: Int, x2: Int, x3: Double) {}
  def tickOptionComputation  (x1: Int, x2: Int, x3: Double, x4: Double, x5: Double, x6: Double, x7: Double, x8: Double, x9: Double, x10: Double) {}
  def tickSize  (x1: Int, x2: Int, x3: Int) {}
    def tickPrice  (tickerId: Int, field: Int, price: Double, canAutoExec: Int) {
      mktDataHandlers(tickerId)(field, price, canAutoExec)    
    }
  // From AnyWrapper
  def connectionClosed() {}

  def error(id: Int, x2: Int, msg: java.lang.String) { 
     idsymbol.get(id) match {
       case Some(symb) => print("[" + symb + "] ")
       case None => print("[id"+id + "] ")
     }
     println("Error: " + msg) 
  }
  def error(x1: java.lang.String) { println("Error: " + x1) }
  def error(x1: java.lang.Exception) { throw x1 }
  }

  def stkContract(symbol: String): Contract = {
    println("New stk contract for " + symbol)
    val c = new Contract
    c.m_symbol = symbol
    c.m_secType = "STK"
    c.m_exchange = "SMART"
    c.m_currency = "USD"
    return c
  }

  def frontMonthExpiration(): String = {
    val c = Calendar.getInstance
    return c.get(Calendar.YEAR).toString + 
           ("%02d" format ((c.get(Calendar.MONTH) / 3 + 1)*3))
  }

  def futContract(symbol: String, exch: String, 
                  expiry: String = frontMonthExpiration()  ): Contract = {
    val c = new Contract
    c.m_symbol = symbol
    c.m_secType = "FUT"
    c.m_exchange = exch
    c.m_currency = "USD"
    c.m_expiry = expiry
    return c 
  }

  def nextReqId(): Int = { 
     id += 1
     return id
  }

  def reqContractDetails(symbol: String, callback: ContractDetails => Unit ) {
    val c = new Contract
    c.m_symbol = symbol
    
    cl.reqContractDetails(nextReqId(), c)
  }

  var idsymbol = new HashMap[Int, String]
  var mktDataHandlers = new HashMap[Int, (Int, Double, Int) => Unit]  
  var prices = new HashMap[String, Map[String, Queue[(Long, Double)]]]

  def reqMktData(symbol: String, assetType: String = "stk") {
    val n_id = nextReqId()
    idsymbol += (n_id -> symbol)
    println("Requesting " + symbol)
    prices += (symbol -> Map("lastPrice" -> Queue[(Long, Double)](),
			     "bidPrice" -> Queue[(Long, Double)](),
			     "askPrice" -> Queue[(Long, Double)]()))
    mktDataHandlers += (n_id -> ((fieldid, price, _) => 
        {
	  //println("Handling tick for " + n_id + " with tick type " + fieldid)
          val wsMsg = Json.stringify(Json.toJson(
                        collection.immutable.Map("symbol" -> symbol, 
                            "price" -> ("%.2f" format price),
			    "ticktype" -> TickType.getField(fieldid))))
          prices(symbol).get(TickType.getField(fieldid)) match {
            case Some(queue) => {
	       val elem = (System.currentTimeMillis, price)
	       if (TickType.getField(fieldid).equals("lastPrice") && 
                   queue.length > 1) {
		 val timeSinceLastTrade = elem._1 - queue.head._1
		 val deltaMsg = Json.stringify(Json.toJson(
				 collection.immutable.Map("symbol" -> symbol,
					"price" -> timeSinceLastTrade.toString,
				        "ticktype" -> "timeDelta")))
		 //sockEnumerator.push(deltaMsg)	
		 channel.push(deltaMsg)

               } 
	       queue.enqueue(elem)
               if (queue.length >= 10) { 
                 queue.dequeue() 
	       }
               
            }
            case None => ()
          }
          //sockEnumerator.push(wsMsg)
		  channel.push(wsMsg)
        }))

    cl.reqMktData(n_id, 
                  assetType match { 
                    case x @ ("globex" | "ecbot" | "nymex") => futContract(symbol, x)
                    case _ => stkContract(symbol) 
                  }, 
                  "", false) 
  }

  def start() {
    println("Starting up Market Data Engine")
    val r0 = cl.eConnect("127.0.0.1", 7496, 0)
    coll.map(x => reqMktData(x("symbol").asInstanceOf[String], 
                             x("assettype").asInstanceOf[String]))
  }

  def getData(symbol : String) = WebSocket.using[String] { request =>
    // Log events to the console
    val in = Iteratee.foreach[String](println).mapDone { _ =>
      println("Disconnected")
    }
  
     // Send a single 'Hello!' message
     val out = Enumerator("Hello!")
     (in, out)
  }

  //var sockEnumerator = Enumerator.imperative[String]()
  val (sockEnumerator, channel) = Concurrent.broadcast[String]

  def sock() = WebSocket.using[String] { request =>
    val in = Iteratee.
             foreach[String](x => { 
                println("Websocket received request " + x) 
                val a = x.split(':')
                val assettype = if (a.length < 2) "stk" else a(1)
		assettype match { 
                  case "ind" => {
		    val holdings = IndexPortfolio(a(0))
		    println("Holdings: " + holdings)
		    holdings.map(reqMktData(_))   
		  }
		  case "del" => {
		    val symid = idsymbol.map(_.swap)
		    cl.cancelMktData(symid(a(0)))
		  }
                  case _ => {
                    val o = MongoDBObject("symbol" -> a(0), 
                                        "assettype" -> assettype) 
                    coll.findOne(o) match {
                      case None => { 
                        coll += o
                        reqMktData(a(0),a(1))
                      }
                      case Some(_) => ()
                    }       
		  }
		}             
                                  }).
            mapDone { _ =>
      println("Disconnected websocket")
    }
  
     // Send a single 'Hello!' message
     val out = sockEnumerator //Enumerator("{\"abc\": 123.00}") >>> Enumerator.eof 
     (in, out)    
  }

  def index() = Action {
    val data = List()
    println("isConnected " + cl.isConnected() )
    if (!cl.isConnected()) {
      start()
    }
    Ok(views.html.mktdata.index(data))
  }

  def delete(symbol : String) = Action {
    coll -= MongoDBObject("symbol" -> symbol)
    Ok("Removed " + symbol)
  }
}

import ProductDB.{AddProduct, AddedProduct, FindAllProducts, FindProduct}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.{Future, duration}
import scala.concurrent.duration.{DurationInt, SECONDS, TimeUnit}
import scala.language.postfixOps

case class Product(name: String, brand: String)

object ProductDB{
  case object FindAllProducts
  case class FindProduct(id: Int)
  case class AddProduct(product: Product)
  case class AddedProduct(id: Int)
}

class ProductService extends Actor with ActorLogging{

  var productsList : Map[Int, Product] = Map()
  var productId: Int = 0

  override def receive: Receive = {
    case FindAllProducts =>
      log.info("finfing all products")
      sender() ! productsList.values.toList
    case FindProduct(id) =>
      log.info(s"founding product by id: $id")
      sender() ! productsList.get(id)
    case AddProduct(product: Product) =>
      log.info("adding new product")
      productsList += (productId -> product)
      sender() ! AddedProduct(productId)
      productId += 1
  }
}

trait ProductJsonProtocol extends DefaultJsonProtocol{

  implicit val productFormat = jsonFormat2(Product) // jsonFormat 2 çünkü Product class'ı 2 parametreye sahip

}

object RestAPIServer extends App with ProductJsonProtocol {

  implicit val system = ActorSystem("ProductRestAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  //JSON marshalling
  val product = Product("iphone 13 pro max" ,"Apple")
  println(product.toJson.prettyPrint)

  //JSON unmarshalling
  val stringProduct =
    """
      |{
      |  "brand": "Apple",
      |  "name": "iphone 13 pro max"
      |}
      |""".stripMargin

  println(stringProduct.parseJson.convertTo[Product])

  val productDb = system.actorOf(Props[ProductService],"productActor") // actor oluşturuyoruz

  val productList = List(
    Product("iphone 13","Apple"),
    Product("iMac","Apple"),
    Product("MacBook pro ","Apple"),
    Product("MacBook air ","Apple")
  )

  productList.foreach { product =>
    productDb ! AddProduct(product)
  }

  // Server
  implicit val defaultTimeOut = Timeout(2 seconds)

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/products"),_,_,_) =>
      val productFuture : Future[List[Product]]= (productDb ? FindAllProducts).mapTo[List[Product]]
      productFuture.map{ products =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            products.toJson.prettyPrint
          )
        )
      }
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse (status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

}

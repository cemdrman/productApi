import ElasticSearchProduct.FindAllProducts
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.elasticsearch.search.sort.SortOrder
import spray.json._

import scala.concurrent.{Future, duration}
import scala.concurrent.duration.{DurationInt, SECONDS, TimeUnit}
import scala.language.postfixOps

case class Product(name: String, item_id: String, local: String, click: Long, purchase: Long)

object ElasticSearchProduct{
  case object FindAllProducts
}

class ProductService extends Actor with ActorLogging{

  val elasticClient = new ElasticClient()

  override def receive: Receive = {
    case FindAllProducts =>
      log.info("finding all products")
      sender() ! elasticClient.findAllProduct(10,0,"name", SortOrder.ASC)
  }
}

object RestAPIServer extends App with ProductJsonProtocol {

  implicit val system = ActorSystem("ProductRestAPI")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val productActor = system.actorOf(Props[ProductService],"productActor") // actor oluşturuyoruz

  val productList = List(
    Product("iphone 13", "1", "China", 250, 50),
    Product("iMac", "2", "China", 250, 50),
    Product("MacBook ", "3", "China", 250, 50),
    Product("MacBook air ", "4", "China", 250, 50)
  )

  val elasticClient = new ElasticClient()

  productList.foreach { product =>
    elasticClient.addProduct(product)
  }

  // Server
  implicit val defaultTimeOut = Timeout(2 seconds)

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/products"),_,_,_) =>
      val productFuture : Future[List[Product]]= (productActor ? FindAllProducts).mapTo[List[Product]]
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

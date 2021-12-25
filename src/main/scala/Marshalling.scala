import spray.json.DefaultJsonProtocol

trait ProductJsonProtocol extends DefaultJsonProtocol{

  implicit val productFormat = jsonFormat5(Product)

}

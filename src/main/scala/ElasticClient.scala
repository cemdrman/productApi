
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.client.PreBuiltTransportClient

import java.net.InetAddress

class ElasticClient extends ProductJsonProtocol {

  private val port = 9300
  private val elasticCluster : String = "docker-cluster"
  private val nodes = List("localhost")
  private val indexName = "productstore"

  lazy val settings = Settings.builder()
    .put("cluster.name", elasticCluster)
    .build()

  val client = new PreBuiltTransportClient(settings)
  nodes.foreach(h => client.addTransportAddress(new TransportAddress(InetAddress.getByName(h), port)))

  def addProduct(product: Product): Unit ={
    client.prepareIndex(indexName,"product", product.item_id)
      .setSource(product, XContentType.JSON).get()
  }

  def findAllProduct(size: Int, pageNumber: Int, sortWith:String, sortType: SortOrder): SearchResponse ={

    client.prepareSearch(indexName)
      .setQuery(QueryBuilders.matchAllQuery())
      .setFrom(pageNumber * size)
      .setSize(size)
      .addSort(sortWith,sortType)
      .execute()
      .actionGet()
  }

}

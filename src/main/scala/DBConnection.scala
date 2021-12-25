import org.elasticsearch.search.sort.SortOrder

import java.sql.{Connection, DriverManager, ResultSet}

class DBConnection {

  val driver = "com.mysql.jdbc.Driver"
  val url = "jdbc:mysql://localhost/mysql"
  val username = "root"
  val password = "password"
  var connection:Connection = null

  def connect(): Unit ={
    try {
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)
    } catch {
      case e => e.printStackTrace
    }
  }

  def findActiveConfig(): SortingConfig ={
    connect()
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery("SELECT * FROM CONFIG where status = active LIMIT 1")
    connection.close()
    convertToConfig(resultSet)
  }
  private val sortingParams: String = "SortingParams"
  private val order: String = "SortOrder"
  private val status: String = "Status"

  private def convertToConfig(resultSet: ResultSet): SortingConfig ={
    val sortType = resultSet.getObject(order) match {
      case "ASC"  => SortOrder.ASC
      case "DESC"  => SortOrder.DESC
    }
    SortingConfig(resultSet.getString(sortingParams), sortType, resultSet.getBoolean(status))
  }

}

import org.elasticsearch.search.sort.SortOrder

case class SortingConfig(sortingParams : String, order: SortOrder, status: Boolean)

package me.roybailey.neo4k.api


typealias QueryData = Map<String,Any?>


interface QueryResult {

    fun row(): Int
    fun columns(): List<String>
    fun data(): QueryData
    fun value(idx: Int) = data().get(columns()[idx])
    fun value(name: String) = data().get(name)

}


data class SimpleQueryResult(val row: Int, val columns: List<String>, val data: QueryData) : QueryResult {

    override fun row(): Int = row

    override fun columns(): List<String> = columns

    override fun data(): QueryData = data

}


typealias QueryResultConsumer = (QueryResult) -> Unit


enum class QueryType {
    CYPHER,
    SQL,
    API,
    CSV
}


typealias QueryExecutor = (query: QueryStatement, params: QueryParams, mapper: QueryResultConsumer) -> Unit

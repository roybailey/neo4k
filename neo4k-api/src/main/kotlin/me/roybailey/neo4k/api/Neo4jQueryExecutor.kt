package me.roybailey.neo4k.api

import me.roybailey.neo4k.dsl.QueryParams
import me.roybailey.neo4k.dsl.QueryResultConsumer
import me.roybailey.neo4k.dsl.QueryStatement
import me.roybailey.neo4k.dsl.SimpleQueryResult
import mu.KotlinLogging
import org.neo4j.graphdb.Node


/**
 * Adaptor between Neo4jService and Query services/models
 */
class Neo4jQueryExecutor(val neo4jService: Neo4jService) {

    val logger = KotlinLogging.logger {}



    /**
     * QueryExecutor method
     * Runs a append query and wraps the Neo4j results in a QueryResult interface
     */
    fun run(query: QueryStatement, params: QueryParams, consumer: QueryResultConsumer) {
        neo4jService.execute(query.toQueryString(params), params) {
            var row = -1
            while (it.hasNext())
                consumer(SimpleQueryResult(++row, it.keys(), it.single().asMap()))
        }
    }


    /**
     * QueryExecutor method
     * Runs a append query and wraps the Neo4j results in a QueryResult interface
     * Additionally flattening any node or map values into columns
     */
    fun runFlatMap(query: QueryStatement, params: QueryParams, consumer: QueryResultConsumer) {

        run(query, params) {
            val flatMapResult = LinkedHashMap<String, Any?>()
            it.columns().forEachIndexed { cdx, name ->

                val value = it.value(name)
                when (value) {
                    is Node -> value.allProperties.forEach { prop ->
                        flatMapResult.put(name + "." + prop.key, prop.value)
                    }
                    is Map<*, *> -> value.keys.forEach { prop ->
                        flatMapResult.put(name + "." + prop, value[prop])
                    }
                    else -> flatMapResult.put(name, value)
                }
            }
            consumer(SimpleQueryResult(it.row(), flatMapResult.keys.toList(), flatMapResult))
        }
    }
}

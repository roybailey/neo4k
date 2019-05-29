package me.roybailey.neo4k.api

import org.neo4j.graphdb.Result


typealias Neo4jResultMapper = (result: Result) -> Unit

val nullNeo4jResultMapper = { _: Result -> }

interface Neo4jService {

    fun shutdown(): Unit
    fun isEmbedded(): Boolean
    fun setStatic(key: String, value: Any, verification: (value: Any) -> Unit)
    fun execute(cypher: String, params: Map<String, Any> = emptyMap(), code: Neo4jResultMapper = nullNeo4jResultMapper)
    fun query(cypher: String, params: Map<String, Any> = emptyMap()): List<Map<String, Any>>
    fun <T> queryForObject(cypher: String, params: Map<String, Any> = emptyMap()): T?

    companion object {

        fun getInstance(
                neo4jUri: String,
                boltConnectorPort: Int = 0,
                ignoreErrorOnDrop: Boolean = true
        ): Neo4jService {
            return Neo4jServiceEmbedded(neo4jUri, boltConnectorPort, ignoreErrorOnDrop)
        }
    }
}

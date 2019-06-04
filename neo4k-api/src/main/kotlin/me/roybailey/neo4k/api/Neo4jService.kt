package me.roybailey.neo4k.api

import org.neo4j.graphdb.Result
import org.neo4j.procedure.Name
import org.neo4j.procedure.UserFunction
import java.util.*


typealias Neo4jResultMapper = (result: Result) -> Unit

val nullNeo4jResultMapper = { _: Result -> }

data class Neo4jServiceOptions(
        val neo4jUri: String,
        val boltPort: Int = -1,
        val username: String = "neo4j",
        val password: String = "",
        val neo4jProcedures: List<Class<*>> = Neo4jService.getDefaultProcedures(),
        val ignoreErrorOnDrop: Boolean = true,
        val ignoreProcedureNotFound: Boolean = true
) {
    val mode : String = neo4jUri.toLowerCase().substring(0,4)
}

interface Neo4jService {

    fun registerProcedures(toRegister: List<Class<*>>): Neo4jService
    fun shutdown(): Unit
    fun isEmbedded(): Boolean
    fun setStatic(key: String, value: Any, verification: (value: Any) -> Unit): Neo4jService
    fun execute(cypher: String, params: Map<String, Any> = emptyMap(), code: Neo4jResultMapper = nullNeo4jResultMapper): Neo4jService
    fun query(cypher: String, params: Map<String, Any> = emptyMap()): List<Map<String, Any>>
    fun <T> queryForObject(cypher: String, params: Map<String, Any> = emptyMap()): T?

    companion object {

        fun getInstance(options: Neo4jServiceOptions): Neo4jService {
            val neo4jService = when(options.mode) {
                "bolt" -> Neo4jServiceBolt(options)
                else -> Neo4jServiceEmbedded(options)
            }
            neo4jService.registerProcedures(options.neo4jProcedures)
            return neo4jService
        }

        fun getDefaultProcedures(): List<Class<out Any>> {
            return listOf(
                    Neo4jServiceProcedures::class.java,
                    apoc.help.Help::class.java,
                    apoc.coll.Coll::class.java,
                    apoc.create.Create::class.java,
                    apoc.index.FulltextIndex::class.java,
                    apoc.path.PathExplorer::class.java,
                    apoc.meta.Meta::class.java,
                    apoc.refactor.GraphRefactoring::class.java,
                    apoc.cache.Static::class.java,
                    apoc.lock.Lock::class.java,
                    apoc.text.Strings::class.java,
                    apoc.date.Date::class.java,
                    apoc.map.Maps::class.java,
                    apoc.convert.Json::class.java,
                    apoc.convert.Convert::class.java,
                    apoc.load.Jdbc::class.java,
                    apoc.load.LoadJson::class.java,
                    apoc.load.Xml::class.java,
                    apoc.periodic.Periodic::class.java
            )
        }
    }
}


class Neo4jServiceProcedures {

    @UserFunction("custom.data.encrypt")
    fun format(@Name("value") value: String,
               @Name(value = "key", defaultValue = "") key: String): String =
            String(Base64.getEncoder().encode(value.toByteArray()))
}

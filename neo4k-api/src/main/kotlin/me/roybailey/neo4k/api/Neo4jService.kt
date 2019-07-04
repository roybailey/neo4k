package me.roybailey.neo4k.api

import me.roybailey.neo4k.bolt.Neo4jBoltService
import me.roybailey.neo4k.embedded.Neo4jEmbeddedService
import org.neo4j.procedure.Name
import org.neo4j.procedure.UserFunction
import java.util.*
import java.util.stream.Stream

interface Neo4jServiceRecord {
    fun keys(): List<String>
    fun values(): List<Any>
    fun containsKey(lookup: String): Boolean
    fun index(lookup: String): Int
    operator fun get(key: String): Any?
    operator fun get(index: Int): Any?
    fun size(): Int
    fun asMap(): Map<String, Any>
    fun fields(): List<Pair<String, Any>>
}

class Neo4jMapRecord(val template: Map<String, Any>, vararg more: Pair<String, Any>) : Neo4jServiceRecord {

    val record: Map<String, Any>

    init {
        record = mutableMapOf<String, Any>().also { it.putAll(template) }.also { it.putAll(more) }.toMap()
    }

    override fun keys(): List<String> = record.entries.map { it.key }
    override fun values(): List<Any> = record.entries.map { it.value }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = keys().indexOf(lookup)
    override fun get(key: String): Any = record.getOrDefault(key, Unit)
    override fun get(index: Int): Any = fields()[index].second
    override fun size(): Int = record.size
    override fun asMap(): Map<String, Any> = record
    override fun fields(): List<Pair<String, Any>> = record.entries.map { Pair(it.key, it.value) }
}

interface Neo4jServiceStatementResult : Iterator<Neo4jServiceRecord> {

    fun address(): String
    fun statement(): String
    fun parameters(): Map<String, Any>
    fun single(): Neo4jServiceRecord
    fun keys(): List<String>
    fun list(): List<Neo4jServiceRecord>
    fun stream(): Stream<Neo4jServiceRecord>
}

typealias Neo4jResultMapper = (record: Neo4jServiceStatementResult) -> Unit
typealias Neo4jRecordMapper<T> = (record: Neo4jServiceRecord) -> T

val nullNeo4jResultMapper = { _: Neo4jServiceStatementResult -> }

data class Neo4jServiceOptions(
        val neo4jUri: String,
        val boltPort: Int = -1,
        val username: String = "neo4j",
        val password: String = "",
        val neo4jProcedures: List<Class<*>> = Neo4jService.getDefaultProcedures(),
        val ignoreErrorOnDrop: Boolean = true,
        val ignoreProcedureNotFound: Boolean = true
) {
    val mode: String = neo4jUri.toLowerCase().substring(0, 4)
}

interface Neo4jService {

    fun registerProcedures(toRegister: List<Class<*>>): Neo4jService
    fun shutdown()
    fun isEmbedded(): Boolean
    fun execute(cypher: String, params: Map<String, Any> = emptyMap(), code: Neo4jResultMapper = nullNeo4jResultMapper): Neo4jService
    fun <T> query(cypher: String, params: Map<String, Any> = emptyMap(), mapper: Neo4jRecordMapper<T>): List<T>
    fun <T> queryForObject(cypher: String, params: Map<String, Any> = emptyMap()): T?

    companion object {

        fun getInstance(options: Neo4jServiceOptions): Neo4jService {
            val neo4jService = when (options.mode) {
                "bolt" -> Neo4jBoltService(options)
                else -> Neo4jEmbeddedService(options)
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

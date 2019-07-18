package me.roybailey.neo4k.api

import me.roybailey.neo4k.bolt.Neo4jBoltService
import me.roybailey.neo4k.dsl.ScriptDsl.apocGetStatic
import me.roybailey.neo4k.dsl.ScriptDsl.apocSetStatic
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
    fun size(): Int
    fun asMap(): Map<String, Any>
    fun fields(): List<Pair<String, Any>>

    operator fun get(key: String): Any?
    operator fun get(index: Int): Any?

    // syntax sugar default value methods

    fun asInt(key: String, defaultValue: Int = 0): Int = get(key)?.let { it as Int } ?: defaultValue

    fun asLong(key: String, defaultValue: Long = 0L): Long = get(key)?.let { it as Long } ?: defaultValue

    fun asBoolean(key: String, defaultValue: Boolean = false): Boolean = get(key)?.let { it as Boolean } ?: defaultValue

    fun asString(key: String, defaultValue: String? = null): String? = get(key)?.let { it.toString() } ?: defaultValue

    fun asFloat(key: String, defaultValue: Float? = null): Float? = get(key)?.let { it as Float } ?: defaultValue

    fun asDouble(key: String, defaultValue: Double? = null): Double? = get(key)?.let { it as Double } ?: defaultValue

    fun asObject(key: String, defaultValue: Any? = null): Any? = get(key)?.let { it } ?: defaultValue

    fun asNumber(key: String, defaultValue: Number? = null): Number? = get(key)?.let { it as Number } ?: defaultValue

    fun asNode(key: String, defaultValue: Neo4jServiceRecord? = null): Neo4jServiceRecord? = get(key)?.let { it as Neo4jServiceRecord }
            ?: defaultValue

    fun <T> asList(key: String, defaultValue: List<T> = emptyList()): List<T> = get(key)?.let { it as List<T> }
            ?: defaultValue

    fun asMap(key: String, defaultValue: Map<String, Any> = emptyMap()): Map<String, Any> = get(key)?.let { it as Map<String, Any> }
            ?: defaultValue

    fun id() = asLong("id")
    fun labels() = asList<String>("labels")
}


val emptyNeo4jServiceRecord = object : Neo4jServiceRecord {
    override fun keys(): List<String> = emptyList()
    override fun values(): List<Any> = emptyList()
    override fun containsKey(lookup: String): Boolean = false
    override fun index(lookup: String): Int = -1
    override fun size(): Int = 0
    override fun asMap(): Map<String, Any> = emptyMap()
    override fun fields(): List<Pair<String, Any>> = emptyList()
    override fun get(key: String): Any? = null
    override fun get(index: Int): Any? = null
}


class Neo4jMapRecord(val template: Map<String, Any>, vararg more: Pair<String, Any>) : Neo4jServiceRecord {

    val record: Map<String, Any> = mutableMapOf<String, Any>().also { it.putAll(template) }.also { it.putAll(more) }.toMap()

    override fun keys(): List<String> = record.entries.map { it.key }
    override fun values(): List<Any> = record.entries.map { it.value }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = keys().indexOf(lookup)
    override fun get(key: String): Any = record.getOrDefault(key, Unit)
    override fun get(index: Int): Any = fields()[index].second
    override fun size(): Int = record.size
    override fun asMap(): Map<String, Any> = record
    override fun fields(): List<Pair<String, Any>> = record.entries.map { Pair(it.key, it.value) }
    override fun toString(): String = super.toString() + keys()
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

    /**
     * Query for a list of records
     *
     * @param cypher - the append query string
     * @param params - the map of append query parameters
     * @param mapper - the record mapper to convert Neo4jServiceRecord objects into <T> objects
     * @return list of <T> objects; otherwise empty list
     */
    fun <T> query(cypher: String, params: Map<String, Any> = emptyMap(), mapper: Neo4jRecordMapper<T>): List<T> {
        val result = mutableListOf<T>()
        execute(cypher, params) { statementResult ->
            while (statementResult.hasNext())
                mapper(statementResult.next())?.let { result.add(it) }
        }
        return result.toList()
    }


    /**
     * Query for an object
     *
     * @param cypher - the append query string
     * @param params - the map of append query parameters
     * @param mapper - the record mapper to convert the first Neo4jServiceRecord object into <T> object;
     * null to take the singular value returned as a primitive
     * @return the object result
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> queryForObject(cypher: String, params: Map<String, Any> = emptyMap(), mapper: Neo4jRecordMapper<T>? = null): T? {
        var result: T? = null
        execute(cypher, params) {
            if (it.hasNext()) {
                val value = it.next()
                result = mapper?.let { it(value) } ?: (value.asMap().entries.first().value as T)
            }
        }
        return result
    }


    /**
     * Sets an apoc static value in the neo4j database, for use in cypher later
     * @param key the name of the static variable to assign
     * @param value the value to assign the static variable
     * @return this service for chaining multiple calls
     */
    fun setStatic(key: String, value: Any): Neo4jService {
        // set static global variables such as sensitive connection values...
        execute(apocSetStatic(key, value.toString()), emptyMap())
        val storedValue = getStatic(key, value)
        if (storedValue != value)
            throw RuntimeException("Failed to assign static key [$key] with value [$value], came back with [$storedValue] instead")
        return this
    }


    /**
     * Gets an apoc static value from the neo4j database
     * @param key the name of the static variable to assign
     * @param defaultValue the value to return if no static variable value is found (defaults to null)
     * @return the static variable value from the database; otherwise the default value provided
     */
    fun getStatic(key: String, defaultValue: Any? = null): Any? {
        var result: Any? = null
        execute(apocGetStatic(key), emptyMap()) {
            if (it.hasNext())
                result = it.single()["value"]
        }
        return result?.let { result } ?: defaultValue
    }


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

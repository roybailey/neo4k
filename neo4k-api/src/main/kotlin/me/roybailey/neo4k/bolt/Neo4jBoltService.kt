package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.*
import mu.KotlinLogging
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import java.net.InetAddress
import java.util.stream.Stream


@Suppress("UNCHECKED_CAST")
open class Neo4jBoltService(val options: Neo4jServiceOptions) : Neo4jService {

    private val LOG = KotlinLogging.logger {}
    private val instanceSignature = InetAddress.getLocalHost().canonicalHostName + "-" + hashCode()

    private val neo4jConfiguration = Neo4jService::class.java.getResource("/neo4j.conf")

    private var driver: Driver

    init {
        LOG.info("########### ########## ########## ########## ##########")
        LOG.info("Creating Neo4j Database options=$options instance=$instanceSignature")
        LOG.info("########### ########## ########## ########## ##########")

        LOG.info("Created Neo4j Database from: $neo4jConfiguration")

        val neo4jUri = if (options.neo4jUri.substring(7).contains(":")) options.neo4jUri else options.neo4jUri + ":" + options.boltPort
        driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(options.username, options.password))

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                driver.close()
            }
        })
    }

    override fun toString(): String = "Neo4jBoltService{ options=$options }"


    override fun shutdown() {
        try {
            LOG.info("########### ########## ########## ########## ##########")
            LOG.info("Shutdown Neo4j Database options=$options instance=${hashCode()}")
            LOG.info("########### ########## ########## ########## ##########")
            driver.close()
        } catch (err: Exception) {
            LOG.warn("Unable to shutdown Neo4j bolt database: $err")
        }
    }


    override fun isEmbedded(): Boolean = false


    /**
     * Since a bolt connection can't inject the procedures,
     * we instead query all installed procedures and attempt to verify all the required procedures exist
     */
    override fun registerProcedures(toRegister: List<Class<*>>): Neo4jService {
        val unregisteredProcedures = mutableSetOf<String>()
        unregisteredProcedures.addAll(toRegister.map { it.name.toLowerCase() })
        query("CALL dbms.procedures()") {
            record -> record.asMap()
        }.map {
            LOG.debug { it }
            it
        }.forEach { procedure ->
            procedure["name"].let { procedureName ->
                if (!unregisteredProcedures.remove(procedureName)) {
                    val packageName = procedureName.toString().substring(0, procedureName.toString().lastIndexOf('.'))
                    val packageProcedures = unregisteredProcedures.filter { it.startsWith(packageName) }
                    packageProcedures.forEach {
                        LOG.debug { "Package found $it" }
                        unregisteredProcedures.remove(it)
                    }
                } else {
                    LOG.debug { "Procedure found $procedureName" }
                }
            }
        }
        unregisteredProcedures.forEach { LOG.error { "Stored Procedure not found using classname or package name $it" } }
        if (!options.ignoreProcedureNotFound && unregisteredProcedures.size > 0) {
            throw RuntimeException("Stored procedures not found $unregisteredProcedures")
        }
        return this
    }


    override fun execute(cypher: String, params: Map<String, Any>, code: Neo4jResultMapper): Neo4jService {
        driver.session().let { session ->
            session.writeTransaction { tx ->
                val result = tx.run(cypher, params)
                code(object : Neo4jServiceStatementResult {
                    override fun address(): String = options.neo4jUri
                    override fun statement(): String = cypher
                    override fun parameters(): Map<String, Any> = params

                    override fun hasNext(): Boolean = result.hasNext()
                    override fun next(): Neo4jServiceRecord = Neo4jBoltRecord(result.next())

                    override fun keys(): List<String> = result.keys()
                    override fun single(): Neo4jServiceRecord = Neo4jBoltRecord(result.single())
                    override fun list(): List<Neo4jServiceRecord> = result.list().map { Neo4jBoltRecord(it) }
                    override fun stream(): Stream<Neo4jServiceRecord> = list().stream()
                })
            }
        }
        return this
    }


    override fun <T> query(cypher: String, params: Map<String, Any>, mapper: Neo4jRecordMapper<T>): List<T> {
        val result = mutableListOf<T>()
        execute(cypher, params) {
            if (it.hasNext())
                result.add(mapper(it.next()))
        }
        return result.toList()
    }


    override fun <T> queryForObject(cypher: String, params: Map<String, Any>): T? {
        var result: T? = null
        execute(cypher, params) {
            if (it.hasNext())
                result = it.next().asMap().entries.first().value as T
        }
        return result
    }

}


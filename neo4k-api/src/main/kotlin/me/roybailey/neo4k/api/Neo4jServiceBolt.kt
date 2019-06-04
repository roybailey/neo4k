package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jCypher.apocGetStatic
import me.roybailey.neo4k.api.Neo4jCypher.apocSetStatic
import mu.KotlinLogging
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import java.lang.RuntimeException
import java.net.InetAddress


@Suppress("UNCHECKED_CAST")
open class Neo4jServiceBolt(val options: Neo4jServiceOptions) : Neo4jService {

    private val LOG = KotlinLogging.logger {}
    private val instanceSignature = InetAddress.getLocalHost().canonicalHostName + "-" + hashCode()

    private val neo4jConfiguration = Neo4jService::class.java.getResource("/neo4j.conf")

    lateinit var driver: Driver

    init {
        LOG.info("########### ########## ########## ########## ##########")
        LOG.info("Creating Neo4j Database options=$options instance=$instanceSignature")
        LOG.info("########### ########## ########## ########## ##########")

        LOG.info("Created Neo4j Database from: $neo4jConfiguration")


        driver = GraphDatabase.driver(options.neo4jUri, AuthTokens.basic(options.username, options.password))

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                driver.close()
            }
        })
    }

    override fun toString(): String = "Neo4jServiceBolt{ options=$options }"


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
        query("CALL dbms.procedures()").map {
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
        if(!options.ignoreProcedureNotFound && unregisteredProcedures.size > 0) {
            throw RuntimeException("Stored procedures not found $unregisteredProcedures")
        }
        return this
    }


    override fun setStatic(key: String, value: Any, verification: (value: Any) -> Unit): Neo4jService {
        // set static global variables such as sensitive connection values...
        execute(apocSetStatic(key, value.toString()), emptyMap()) {
            LOG.info { it.next() }
        }
        execute(apocGetStatic(key), emptyMap()) {
            val savedValue = it.next().getValue("value")
            verification(savedValue)
        }
        return this
    }


    override fun execute(cypher: String, params: Map<String, Any>, code: Neo4jResultMapper): Neo4jService {

        driver.session().let { session ->
            session.writeTransaction { tx ->
                val result = tx.run(cypher, params)
                // todo code()
            }
        }
        return this
    }


    override fun query(cypher: String, params: Map<String, Any>): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        driver.session().let { session ->
            session.writeTransaction { tx ->
                val srs = tx.run(cypher, params)
                while (srs.hasNext()) {
                    val record = srs.next()
                    result.add(record.asMap())
                }
            }
        }
        return result.toList()
    }


    override fun <T> queryForObject(cypher: String, params: Map<String, Any>): T? {
        var result: T? = null
        driver.session().let { session ->
            session.writeTransaction { tx ->
                val srs = tx.run(cypher, params)
                if (srs.hasNext()) {
                    result = srs.next().asMap().entries.first().value as T
                }
            }
        }
        return result
    }

}


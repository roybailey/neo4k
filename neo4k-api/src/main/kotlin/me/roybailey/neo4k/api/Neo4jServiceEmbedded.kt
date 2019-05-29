package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jCypher.apocGetStatic
import me.roybailey.neo4k.api.Neo4jCypher.apocSetStatic
import mu.KotlinLogging
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.internal.GraphDatabaseAPI
import org.neo4j.procedure.Name
import org.neo4j.procedure.UserFunction
import java.io.File
import java.net.InetAddress
import java.time.Instant.now
import java.util.*


@Suppress("UNCHECKED_CAST")
open class Neo4jServiceEmbedded(
        val neo4jUri: String,
        val boltConnectorPort: Int = 0,
        val ignoreErrorOnDrop: Boolean = true
) : Neo4jService {

    private val log = KotlinLogging.logger {}
    private val instanceSignature = InetAddress.getLocalHost().canonicalHostName + "-" + hashCode()

    private val neo4jConfiguration = Neo4jService::class.java.getResource("/neo4j.conf")

    private var neo4jDatabaseFolder: File
    lateinit var graphDb: GraphDatabaseService

    init {
        log.info("########### ########## ########## ########## ##########")
        log.info("Creating Neo4j Database with neo4jUri=$neo4jUri instance=$instanceSignature")
        log.info("########### ########## ########## ########## ##########")

        neo4jDatabaseFolder = File(when {
            neo4jUri.trim().isEmpty() -> createTempDir("${Neo4jService::class.simpleName}-", "-$instanceSignature").canonicalPath
            neo4jUri.startsWith("file://") -> File(neo4jUri.substring("file://".length)).canonicalPath
            else -> throw IllegalArgumentException("neo4jUri must be file:// based as only embedded instance supported")
        }.replace("{timestamp}", now().toString()) + "/graph.db")
        log.info("Creating Neo4j Database at $neo4jDatabaseFolder")

        val graphDbBuilder = GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(neo4jDatabaseFolder)
                .loadPropertiesFromURL(neo4jConfiguration)

        if (boltConnectorPort > 0) {
            val bolt = BoltConnector("0")
            val boltListenAddress = "0.0.0.0:$boltConnectorPort"
            val boltAdvertisedAddress = InetAddress.getLocalHost().hostName + ":" + boltConnectorPort
            graphDbBuilder.setConfig(bolt.type, "BOLT")
                    .setConfig(bolt.enabled, "true")
                    .setConfig(bolt.listen_address, boltListenAddress)
                    .setConfig(bolt.advertised_address, boltAdvertisedAddress)
            log.info("Creating Neo4j Bolt Connector on Port : $boltConnectorPort")
            log.info("Creating Neo4j Bolt Listen Address : $boltListenAddress")
            log.info("Creating Neo4j Bolt Advertised Address : $boltAdvertisedAddress")
        }

        try {
            graphDb = graphDbBuilder.newGraphDatabase()
        } catch (err: Exception) {
            log.error("########### ########## ########## ########## ##########")
            log.error("!!!!!!!!!! Error creating Neo4j Database !!!!!!!!!!")
            log.error("Error creating Neo4j Database", err)
            log.error("########### ########## ########## ########## ##########")
            System.exit(-1)
        }

        registerDefaultProcedures()

        log.info("Created Neo4j Database from: $neo4jConfiguration")

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                shutdown()
            }
        })
    }

    private fun registerDefaultProcedures() {
        registerProcedures(listOf(
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
        ))
        log.info("Registered Neo4j Apoc Procedures")
    }


    override fun toString(): String = "Neo4jService{ neo4jUri=$neo4jUri, neo4jDatabaseFolder=$neo4jDatabaseFolder, boltConnectorPort=$boltConnectorPort }"


    override fun shutdown() {
        try {
            log.info("########### ########## ########## ########## ##########")
            log.info("Shutdown Neo4j Database with neo4jUri=$neo4jUri instance=${hashCode()}")
            log.info("########### ########## ########## ########## ##########")
            graphDb.shutdown()
        } catch (err: Exception) {
            log.warn("Unable to shutdown Neo4j embedded database: $err")
        }
    }


    override fun isEmbedded(): Boolean = neo4jUri.startsWith("file://")


    fun registerProcedures(toRegister: List<Class<*>>) {
        if (isEmbedded()) {
            val procedures = (graphDb as GraphDatabaseAPI).dependencyResolver.resolveDependency(Procedures::class.java)
            toRegister.forEach { proc ->
                try {
                    procedures.registerProcedure(proc, true)
                    procedures.registerFunction(proc, true)
                } catch (e: Exception) {
                    throw RuntimeException("Error registering " + proc, e)
                }
            }
        }
    }

    override fun setStatic(key: String, value: Any, verification: (value: Any) -> Unit) {
        // set static global variables such as sensitive connection values...
        execute(apocSetStatic(key, value.toString()), emptyMap()) {
            log.info { it.next() }
        }
        execute(apocGetStatic(key), emptyMap()) {
            val savedValue = it.next().getValue("value")
            verification(savedValue)
        }
    }


    override fun execute(cypher: String, params: Map<String, Any>, code: Neo4jResultMapper) {
        graphDb.beginTx().run {
            try {
                code(graphDb.execute(cypher, params))
                success()
            } catch (err: Exception) {
                // don't throw errors on cypher drop commands
                if (ignoreErrorOnDrop && cypher.trim().startsWith("drop", ignoreCase = true) &&
                        err.toString().toLowerCase().contains("no such index"))
                    log.warn { "Ignoring failed drop error on : $cypher\n${err.message}" }
                else
                    throw err
            }
        }
    }


    override fun query(cypher: String, params: Map<String, Any>): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        graphDb.beginTx().run {
            val srs = graphDb.execute(cypher, params)

            while (srs.hasNext()) {
                val record = srs.next()
                result.add(record)
            }
            success()
        }
        return result.toList()
    }


    override fun <T> queryForObject(cypher: String, params: Map<String, Any>): T? {
        var result: T? = null
        graphDb.beginTx().run {
            val srs = graphDb.execute(cypher, params)
            if (srs.hasNext()) {
                result = srs.next().entries.first().value as T
            }
            success()
        }
        return result
    }

}

class Neo4jServiceProcedures {

    @UserFunction("custom.data.encrypt")
    fun format(@Name("value") value: String,
               @Name(value = "key", defaultValue = "") key: String): String =
            String(Base64.getEncoder().encode(value.toByteArray()))
}

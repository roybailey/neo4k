package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jCypher.apocGetStatic
import me.roybailey.neo4k.api.Neo4jCypher.apocSetStatic
import mu.KotlinLogging
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.internal.GraphDatabaseAPI
import java.io.File
import java.net.InetAddress
import java.time.Instant.now


@Suppress("UNCHECKED_CAST")
open class Neo4jServiceEmbedded(val options: Neo4jServiceOptions) : Neo4jService {

    private val LOG = KotlinLogging.logger {}
    private val instanceSignature = InetAddress.getLocalHost().canonicalHostName + "-" + hashCode()

    private val neo4jConfiguration = Neo4jService::class.java.getResource("/neo4j.conf")

    private var neo4jDatabaseFolder: File
    lateinit var graphDb: GraphDatabaseService

    init {
        val (neo4jUri, boltPort) = options
        LOG.info("########### ########## ########## ########## ##########")
        LOG.info("Creating Neo4j Database with neo4jUri=$neo4jUri instance=$instanceSignature")
        LOG.info("########### ########## ########## ########## ##########")

        neo4jDatabaseFolder = File(when {
            neo4jUri.trim().isEmpty() -> createTempDir("${Neo4jService::class.simpleName}-", "-$instanceSignature").canonicalPath
            neo4jUri.startsWith("file://") -> File(neo4jUri.substring("file://".length)).canonicalPath
            else -> throw IllegalArgumentException("neo4jUri must be file:// based as only embedded instance supported")
        }.replace("{timestamp}", now().toString()) + "/graph.db")
        LOG.info("Creating Neo4j Database at $neo4jDatabaseFolder")

        val graphDbBuilder = GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(neo4jDatabaseFolder)
                .loadPropertiesFromURL(neo4jConfiguration)

        if (boltPort > 0) {
            val bolt = BoltConnector("0")
            val boltListenAddress = "0.0.0.0:$boltPort"
            val boltAdvertisedAddress = InetAddress.getLocalHost().hostName + ":" + boltPort
            graphDbBuilder.setConfig(bolt.type, "BOLT")
                    .setConfig(bolt.enabled, "true")
                    .setConfig(bolt.listen_address, boltListenAddress)
                    .setConfig(bolt.advertised_address, boltAdvertisedAddress)
            LOG.info("Creating Neo4j Bolt Connector on Port : $boltPort")
            LOG.info("Creating Neo4j Bolt Listen Address : $boltListenAddress")
            LOG.info("Creating Neo4j Bolt Advertised Address : $boltAdvertisedAddress")
        }

        try {
            graphDb = graphDbBuilder.newGraphDatabase()
        } catch (err: Exception) {
            LOG.error("########### ########## ########## ########## ##########")
            LOG.error("!!!!!!!!!! Error creating Neo4j Database !!!!!!!!!!")
            LOG.error("Error creating Neo4j Database", err)
            LOG.error("########### ########## ########## ########## ##########")
            System.exit(-1)
        }

        LOG.info("Created Neo4j Database from: $neo4jConfiguration")

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                shutdown()
            }
        })
    }


    override fun toString(): String = "Neo4jServiceEmbedded{ options=$options, neo4jDatabaseFolder=$neo4jDatabaseFolder }"


    override fun shutdown() {
        try {
            LOG.info("########### ########## ########## ########## ##########")
            LOG.info("Shutdown Neo4j Database options=$options instance=${hashCode()}")
            LOG.info("########### ########## ########## ########## ##########")
            graphDb.shutdown()
        } catch (err: Exception) {
            LOG.warn("Unable to shutdown Neo4j embedded database: $err")
        }
    }


    override fun isEmbedded(): Boolean = options.neo4jUri.startsWith("file://")


    override fun registerProcedures(toRegister: List<Class<*>>): Neo4jService {
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
        graphDb.beginTx().run {
            try {
                code(graphDb.execute(cypher, params))
                success()
            } catch (err: Exception) {
                // don't throw errors on cypher drop commands
                if (options.ignoreErrorOnDrop && cypher.trim().startsWith("drop", ignoreCase = true) &&
                        err.toString().toLowerCase().contains("no such index"))
                    LOG.warn { "Ignoring failed drop error on : $cypher\n${err.message}" }
                else
                    throw err
            }
        }
        return this
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


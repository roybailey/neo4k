package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*
import mu.KotlinLogging
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import org.neo4j.kernel.impl.proc.Procedures
import org.neo4j.kernel.internal.GraphDatabaseAPI
import java.io.File
import java.net.InetAddress
import java.time.Instant.now
import java.util.stream.Stream
import kotlin.system.exitProcess


@Suppress("UNCHECKED_CAST")
open class Neo4jEmbeddedService(val options: Neo4jServiceOptions) : Neo4jService {

    private val logger = KotlinLogging.logger {}
    private val instanceSignature = InetAddress.getLocalHost().canonicalHostName + "-" + hashCode()

    private val neo4jConfiguration = Neo4jService::class.java.getResource("/neo4j.conf")

    private var neo4jDatabaseFolder: File
    lateinit var graphDb: GraphDatabaseService

    init {
        val (neo4jUri, boltPort) = options
        logger.info("########### ########## ########## ########## ##########")
        logger.info("Creating Neo4j Database with neo4jUri=$neo4jUri instance=$instanceSignature")
        logger.info("########### ########## ########## ########## ##########")

        neo4jDatabaseFolder = File(when {
            neo4jUri.trim().isEmpty() -> createTempDir("${Neo4jService::class.simpleName}-", "-$instanceSignature").canonicalPath
            neo4jUri.startsWith("file://") -> File(neo4jUri.substring("file://".length)).canonicalPath
            else -> throw IllegalArgumentException("neo4jUri must be file:// based as only embedded instance supported")
        }.replace("{timestamp}", now().toString()) + "/graph.db")
        logger.info("Creating Neo4j Database at $neo4jDatabaseFolder")

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
            logger.info("Creating Neo4j Bolt Connector on Port : $boltPort")
            logger.info("Creating Neo4j Bolt Listen Address : $boltListenAddress")
            logger.info("Creating Neo4j Bolt Advertised Address : $boltAdvertisedAddress")
        }

        try {
            graphDb = graphDbBuilder.newGraphDatabase()
        } catch (err: Exception) {
            logger.error("########### ########## ########## ########## ##########")
            logger.error("!!!!!!!!!! Error creating Neo4j Database !!!!!!!!!!")
            logger.error("Error creating Neo4j Database", err)
            logger.error("########### ########## ########## ########## ##########")
            exitProcess(-1)
        }

        logger.info("Created Neo4j Database from: $neo4jConfiguration")

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                shutdown()
            }
        })
    }


    override fun toString(): String = "Neo4jEmbeddedService{ options=$options, neo4jDatabaseFolder=$neo4jDatabaseFolder }"


    override fun shutdown() {
        try {
            logger.info("########### ########## ########## ########## ##########")
            logger.info("Shutdown Neo4j Database options=$options instance=${hashCode()}")
            logger.info("########### ########## ########## ########## ##########")
            graphDb.shutdown()
        } catch (err: Exception) {
            logger.warn("Unable to shutdown Neo4j embedded database: $err")
        }
    }


    override fun isEmbedded(): Boolean = options.neo4jUri.startsWith("file://")


    override fun registerProcedures(toRegister: List<Class<*>>): Neo4jService {
        if (isEmbedded()) {
            // todo replace this deprecated method with selection strategy
            val procedures = (graphDb as GraphDatabaseAPI).dependencyResolver.resolveDependency(Procedures::class.java)
            toRegister.forEach { proc ->
                try {
                    procedures.registerProcedure(proc, true)
                    procedures.registerFunction(proc, true)
                } catch (e: Exception) {
                    throw RuntimeException("Error registering $proc", e)
                }
            }
        }
        return this
    }


    override fun execute(cypher: String, params: Map<String, Any>, code: Neo4jResultMapper): Neo4jService {
        graphDb.beginTx().run {
            try {
                val result = graphDb.execute(cypher, params)
                success()
                code(object : Neo4jServiceStatementResult {
                    override fun address(): String = neo4jDatabaseFolder.toString()
                    override fun statement(): String = cypher
                    override fun parameters(): Map<String, Any> = params

                    override fun hasNext(): Boolean = result.hasNext()
                    override fun next(): Neo4jServiceRecord = Neo4jEmbeddedRecord(result.next())

                    override fun keys(): List<String> = result.columns()
                    override fun single(): Neo4jServiceRecord = Neo4jEmbeddedRecord(result.next())
                    override fun list(): List<Neo4jServiceRecord> {
                        val list = mutableListOf<Neo4jEmbeddedRecord>()
                        while (result.hasNext()) {
                            list.add(Neo4jEmbeddedRecord(result.next()))
                        }
                        return list
                    }

                    override fun stream(): Stream<Neo4jServiceRecord> = list().stream()
                })
            } catch (err: Exception) {
                // don't throw errors on append drop commands
                if (options.ignoreErrorOnDrop && cypher.trim().startsWith("drop", ignoreCase = true) &&
                        err.toString().toLowerCase().contains("no such index"))
                    logger.warn { "Ignoring failed drop error on : $cypher\n${err.message}" }
                else
                    throw err
            } finally {
                this.close()
            }
        }
        return this
    }

}


package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*
import me.roybailey.neo4k.bolt.Neo4jBoltService
import me.roybailey.neo4k.dsl.CypherDsl
import mu.KotlinLogging
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.configuration.connectors.BoltConnector
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.graphdb.GraphDatabaseService
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

    lateinit private var neo4jDatabaseFolder: File
    lateinit var graphDbService: DatabaseManagementService
    lateinit var graphDb: GraphDatabaseService
    lateinit var neo4jBoltService: Neo4jBoltService

    init {
        val (neo4jUri, boltPort) = options
        logger.info("########### ########## ########## ########## ##########")
        logger.info("Creating Neo4j Database with neo4jUri=$neo4jUri instance=$instanceSignature")
        logger.info("########### ########## ########## ########## ##########")

        neo4jDatabaseFolder = File(
            when {
                neo4jUri.trim().isEmpty() -> createTempDir(
                    "${Neo4jService::class.simpleName}-",
                    "-$instanceSignature"
                ).canonicalPath
                neo4jUri.startsWith("file://") -> File(neo4jUri.substring("file://".length)).canonicalPath
                else -> throw IllegalArgumentException("neo4jUri must be file:// based as only embedded instance supported")
            }.replace("{timestamp}", now().toString()) + "/graph.db"
        )
        logger.info("Creating Neo4j Database at $neo4jDatabaseFolder")

        val graphDbBuilder = DatabaseManagementServiceBuilder(neo4jDatabaseFolder)
            .loadPropertiesFromFile(neo4jConfiguration.toURI().toString())
//        val graphDbBuilderOld = GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(neo4jDatabaseFolder)
//                .loadPropertiesFromURL(neo4jConfiguration)

        if (boltPort > 0) {
            // val bolt = BoltConnector()
            val boltListenAddress = "0.0.0.0"
            val boltAdvertisedAddress = InetAddress.getLocalHost().hostName
            graphDbBuilder
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, SocketAddress(boltListenAddress, boltPort))
                .setConfig(
                    BoltConnector.advertised_address,
                    SocketAddress(InetAddress.getLocalHost().hostName, boltPort)
                )
//                    .setConfig(bolt.type, "BOLT")
//                    .setConfig(bolt.enabled, "true")
//                    .setConfig(bolt.listen_address, boltListenAddress)
//                    .setConfig(bolt.advertised_address, boltAdvertisedAddress)
            logger.info("Creating Neo4j Bolt Connector on Port : $boltPort")
            logger.info("Creating Neo4j Bolt Listen Address : $boltListenAddress")
            logger.info("Creating Neo4j Bolt Advertised Address : $boltAdvertisedAddress")
        }

        try {
            graphDbService = graphDbBuilder.build()
            graphDb = graphDbService.database(DEFAULT_DATABASE_NAME);
        } catch (err: Exception) {
            logger.error("########### ########## ########## ########## ##########")
            logger.error("!!!!!!!!!! Error creating Neo4j Database !!!!!!!!!!")
            logger.error("Error creating Neo4j Database", err)
            logger.error("########### ########## ########## ########## ##########")
            exitProcess(-1)
        }

        logger.info("Created Neo4j Database from: $neo4jConfiguration")

        neo4jBoltService = Neo4jBoltService(
            Neo4jServiceOptions(
                "bolt://0.0.0.0", boltPort, "neo4j", "", emptyList()
            )
        )
        logger.info("Created Neo4j Bolt Connection: $neo4jBoltService")

        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                shutdown()
            }
        })
    }


    override fun toString(): String =
        "Neo4jEmbeddedService{ options=$options, neo4jDatabaseFolder=$neo4jDatabaseFolder }"


    override fun shutdown() {
        try {
            logger.info("########### ########## ########## ########## ##########")
            logger.info("Shutdown Neo4j Database options=$options instance=${hashCode()}")
            logger.info("########### ########## ########## ########## ##########")
            graphDbService.shutdown()
        } catch (err: Exception) {
            logger.warn("Unable to shutdown Neo4j embedded database: $err")
        }
    }


    override fun isEmbedded(): Boolean = options.neo4jUri.startsWith("file://")


    override fun registerProcedures(toRegister: List<Class<*>>): Neo4jService {
        if (isEmbedded()) {
            val procedures = graphDb
                .let { it as GraphDatabaseAPI }
                .dependencyResolver
                .resolveDependency(org.neo4j.kernel.api.procedure.GlobalProcedures::class.java)

            toRegister.forEach { proc ->
                try {
                    logger.info { "Registering Procedures $proc" }
                    procedures.registerProcedure(proc, true)
                    procedures.registerFunction(proc, true)

                } catch (e: Exception) {
                    throw RuntimeException("Error registering $proc", e)
                }
            }
        }

        val name = "registerProcedures"
        val value = "aaahhhhh!!!!!!" // toRegister.map { it.name }.reduce { acc, nxt -> "$acc,$nxt" }
        graphDb.beginTx().run {
            try {
                // save a static value through apoc query
                val resultSetApoc = this.execute(CypherDsl.apocSetStatic(name, value), emptyMap())
                logger.info { "apoc.static.set($name,$value)\n"+resultSetApoc.resultAsString() }
            } catch (err: Exception) {
                throw err
            } finally {
                this.commit()
                this.close()
            }
        }
        graphDb.beginTx().run {
            try {
                // save a static value through apoc query
                val resultSetApoc = this.execute(CypherDsl.apocSetStatic(name, value), emptyMap())
                logger.info { "apoc.static.set($name,$value)\n"+resultSetApoc.resultAsString() }
            } catch (err: Exception) {
                throw err
            } finally {
                this.commit()
                this.close()
            }
        }
        graphDb.beginTx().run {
            try {
                val resultGetApoc = this.execute("call apoc.static.list('')", emptyMap())
                logger.info { "apoc.static.list('')\n"+resultGetApoc.resultAsString() }
            } catch (err: Exception) {
                throw err
            } finally {
                this.close()
            }
        }
        graphDb.beginTx().run {
            try {
                val resultGetApoc = this.execute(CypherDsl.apocGetStatic(name), emptyMap())
                logger.info { "apoc.static.get($name)\n"+resultGetApoc.hasNext()+":"+resultGetApoc.next() }
                //logger.info { "apoc.static.get($name)\n"+resultGetApoc.resultAsString() }
            } catch (err: Exception) {
                throw err
            } finally {
                this.close()
            }
        }
        neo4jBoltService.execute(CypherDsl.apocSetStatic(name, value), emptyMap())
        neo4jBoltService.execute(CypherDsl.apocGetStatic(name), emptyMap()) { record: Neo4jServiceStatementResult ->
            logger.info { record.next().get("value") }
        }
        return this
    }


    override fun execute(cypher: String, params: Map<String, Any>, code: Neo4jResultMapper): Neo4jService {

        neo4jBoltService.execute(cypher, params, code)
        /*
        graphDb.beginTx().run {
            try {
                val result = this.execute(cypher, params)
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
                    err.toString().toLowerCase().contains("no such index")
                )
                    logger.warn { "Ignoring failed drop error on : $cypher\n${err.message}" }
                else
                    throw err
            } finally {
                this.close()
            }
        }
        */
        return this
    }

}


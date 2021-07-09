package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.dsl.CypherDsl
import mu.KotlinLogging
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.configuration.connectors.BoltConnector
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.kernel.internal.GraphDatabaseAPI
import java.io.File
import java.net.InetAddress


/**
 * Direct driver sample code to investigate query/result structure/performance outside the Neo4jService solution
 */
class Neo4jEmbeddedTest(
    neo4jUri: String,
    boltPort: Int = 7988,
    neo4jConfiguration: String = "/neo4j.conf"
) {

    private val logger = KotlinLogging.logger {}

    lateinit var graphDbService: DatabaseManagementService
    lateinit var graphDb: GraphDatabaseService

    init {
        val graphDbBuilder = DatabaseManagementServiceBuilder(File(neo4jUri).normalize())
            .loadPropertiesFromFile(Neo4jEmbeddedTest::class.java.getResource((neo4jConfiguration)).toURI().toString())
//        val graphDbBuilder = GraphDatabaseFactory()
//                .newEmbeddedDatabaseBuilder(File(neo4jUri))
//                .loadPropertiesFromURL(Neo4jEmbeddedTest::class.java.getResource((neo4jConfiguration)))

        if (boltPort > 0) {
            //val bolt = BoltConnector()
            val boltListenAddress = "0.0.0.0"
            val boltAdvertisedAddress = InetAddress.getLocalHost().hostName
            graphDbBuilder
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, SocketAddress(boltListenAddress, boltPort))
                .setConfig(
                    BoltConnector.advertised_address,
                    SocketAddress(InetAddress.getLocalHost().hostName, boltPort)
                )
//                .setConfig(bolt.type, "BOLT")
//                .setConfig(bolt.enabled, "true")
//                .setConfig(bolt.listen_address, boltListenAddress)
//                .setConfig(bolt.advertised_address, boltAdvertisedAddress)
            logger.info("Creating Neo4j Bolt Connector on Port : $boltPort")
            logger.info("Creating Neo4j Bolt Listen Address : $boltListenAddress")
            logger.info("Creating Neo4j Bolt Advertised Address : $boltAdvertisedAddress")
        }

        try {
            graphDbService = graphDbBuilder.build()
            graphDb = graphDbService.database(DEFAULT_DATABASE_NAME)
            logger.info("Created Neo4j Database $DEFAULT_DATABASE_NAME")
        } catch (err: Exception) {
            logger.error("########### ########## ########## ########## ##########")
            logger.error("!!!!!!!!!! Error creating Neo4j Database !!!!!!!!!!")
            logger.error("Error creating Neo4j Database", err)
            logger.error("########### ########## ########## ########## ##########")
            System.exit(-1)
        }
    }

    fun sampleObjectQuery() {
        data class PersonResult(val id: Long, val name: String, val born: Long)
        data class MovieResult(
            val id: Long,
            val title: String,
            val released: Long,
            val directors: MutableList<PersonResult>
        )

        val cypher = Neo4jEmbeddedTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        graphDb.executeTransactionally(cypher)

        val mapMovies = mutableMapOf<String, MovieResult>()
        val mapDirectors = mutableMapOf<String, PersonResult>()

        graphDb.executeTransactionally("match (m:Movie)-[:DIRECTED]-(d:Person) return m, d", emptyMap()) { result ->
            while (result.hasNext()) {
                val record = result.next()
                logger.info { record }
                val movie = (record["m"] as Node)
                val director = (record["d"] as Node)
                logger.info { "movieId=${movie.id} directorId=${director.id}" }
                logger.info { "movie.labels=${movie.labels} director.labels=${director.labels}" }

                if (!mapDirectors.containsKey(director.id.toString()))
                    mapDirectors[director.id.toString()] = PersonResult(
                        director.id,
                        (director.getProperty("name")!!) as String,
                        (director.getProperty("born")!!) as Long
                    )

                if (!mapMovies.containsKey(movie.id.toString()))
                    mapMovies[movie.id.toString()] = MovieResult(
                        movie.id,
                        (movie.getProperty("title")!!) as String,
                        (movie.getProperty("released")!!) as Long,
                        mutableListOf()
                    )

                mapMovies[movie.id.toString()]?.directors?.add(mapDirectors[director.id.toString()]!!)
            }
        }

        mapMovies.forEach { logger.info { it } }
    }

    fun sampleApocQuery() {

        val procedures = graphDb
            .let { it as GraphDatabaseAPI }
            .dependencyResolver
            .resolveDependency(org.neo4j.kernel.api.procedure.GlobalProcedures::class.java)
        val toRegister = listOf<Class<*>>(apoc.cache.Static::class.java)

        toRegister.forEach { proc ->
            try {
                procedures.registerProcedure(proc, true)
                procedures.registerFunction(proc, true)

            } catch (e: Exception) {
                throw RuntimeException("Error registering $proc", e)
            }
        }

        val cypherSetStatic = CypherDsl.apocSetStatic("test","abc")//"call apoc.static.set('test', 'abc')"
        logger.info { cypherSetStatic }
//        graphDb.executeTransactionally(cypherSetStatic, emptyMap()) { result ->
//            while (result.hasNext()) {
//                val record = result.next()
//                logger.info { record }
//            }
//        }

        graphDb.beginTx().run {
            try {
                val result = execute(cypherSetStatic, emptyMap());
                while (result.hasNext()) {
                    val record = result.next()
                    logger.info { record }
                }
            } catch(err: Exception) {
                logger.error { err }
            } finally {
                this.close()
            }
        }

        val cypherGetStatic = CypherDsl.apocGetStatic("test")//"call apoc.static.get('test')"
        logger.info { cypherGetStatic }
//        graphDb.executeTransactionally(cypherGetStatic, emptyMap()) { result ->
//            while (result.hasNext()) {
//                val record = result.next()
//                logger.info { record }
//            }
//        }
        graphDb.beginTx().run {
            try {
                val result = execute(cypherGetStatic, emptyMap())
                while (result.hasNext()) {
                    val record = result.next()
                    logger.info { record }
                }
            } catch(err: Exception) {
                logger.error { err }
            } finally {
                this.close()
            }
        }

    }

    fun shutdown() {
        graphDbService.shutdown()
        logger.info { "Closed Neo4j connection" }
    }
}


fun main(args: Array<String>) {

    val neo4jUri = if (args.isNotEmpty()) args[0] else "./target/neo4j/EmbeddedServiceBasicTest"
    val neo4j = Neo4jEmbeddedTest(neo4jUri = neo4jUri)
    neo4j.sampleObjectQuery()
    neo4j.sampleApocQuery()
    neo4j.shutdown()
}

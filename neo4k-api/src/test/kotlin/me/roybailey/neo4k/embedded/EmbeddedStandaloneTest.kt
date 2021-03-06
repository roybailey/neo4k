package me.roybailey.neo4k.embedded

import mu.KotlinLogging
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import java.io.File
import java.net.InetAddress


/**
 * Direct driver sample code to investigate query/result structure/performance outside the Neo4jService solution
 */
class Neo4jEmbeddedTest(
        neo4jUri: String,
        boltPort: Int = 7988,
        neo4jConfiguration: String = "/neo4j.conf") {

    private val logger = KotlinLogging.logger {}

    lateinit var graphDb: GraphDatabaseService

    init {
        val graphDbBuilder = GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(File(neo4jUri))
                .loadPropertiesFromURL(Neo4jEmbeddedTest::class.java.getResource((neo4jConfiguration)))

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
            System.exit(-1)
        }

        val cypher = Neo4jEmbeddedTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        graphDb.execute(cypher)
    }

    fun sampleObjectQuery() {
        data class PersonResult(val id: Long, val name: String, val born: Long)
        data class MovieResult(val id: Long, val title: String, val released: Long, val directors: MutableList<PersonResult>)

        val mapMovies = mutableMapOf<String, MovieResult>()
        val mapDirectors = mutableMapOf<String, PersonResult>()

        graphDb.beginTx().run {
            val result = graphDb.execute("match (m:Movie)-[:DIRECTED]-(d:Person) return m, d", emptyMap())
            while (result.hasNext()) {
                val record = result.next()
                logger.info { record }
                val movie = (record["m"] as Node)
                val director = (record["d"] as Node)
                logger.info { "movieId=${movie.id} directorId=${director.id}" }
                logger.info { "movie.labels=${movie.labels} director.labels=${director.labels}" }

                if (!mapDirectors.containsKey(director.id.toString()))
                    mapDirectors[director.id.toString()] = PersonResult(director.id,
                            (director.getProperty("name")!!) as String,
                            (director.getProperty("born")!!) as Long)

                if (!mapMovies.containsKey(movie.id.toString()))
                    mapMovies[movie.id.toString()] = MovieResult(movie.id,
                            (movie.getProperty("title")!!) as String,
                            (movie.getProperty("released")!!) as Long,
                            mutableListOf())

                mapMovies[movie.id.toString()]?.directors?.add(mapDirectors[director.id.toString()]!!)
            }

            success()
        }

        mapMovies.forEach { logger.info { it } }
    }

    fun shutdown() {
        graphDb.shutdown()
        logger.info { "Closed Neo4j connection" }
    }
}


fun main(args: Array<String>) {

    val neo4jUri = if (args.isNotEmpty()) args[0] else "file://./target/neo4j/EmbeddedServiceBasicTest"
    val neo4j = Neo4jEmbeddedTest(neo4jUri = neo4jUri)
    neo4j.sampleObjectQuery()
    neo4j.shutdown()
}

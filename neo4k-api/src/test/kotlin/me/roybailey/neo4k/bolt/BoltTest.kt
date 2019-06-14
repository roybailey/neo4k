package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.Neo4jCypher
import mu.KotlinLogging
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.driver.v1.Session
import org.neo4j.driver.v1.Value


private val LOG = KotlinLogging.logger("Neo4jBoltTest")


class Neo4jBoltTest(neo4jUri: String, username: String, password: String) {

    private val driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password))

    init {
        LOG.info { "Created Neo4j session $driver" }

        driver.session().run(Neo4jCypher.deleteAllData())
        val cypher = Neo4jBoltTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        driver.session().writeTransaction { tx -> tx.run(cypher) }
        val total = driver.session().run("match (m) return count(m) as total").single().get("total").asLong()
        LOG.info { "Loaded movie database $total" }
    }

    fun session(): Session = driver.session()

    fun sampleObjectQuery() {
        data class PersonResult(val id: Long, val name: String, val born: Long)
        data class MovieResult(val id: Long, val title: String, val released: Long, val directors: MutableList<PersonResult>)

        val mapMovies = mutableMapOf<String, MovieResult>()
        val mapDirectors = mutableMapOf<String, PersonResult>()

        session().run {
            val statementResult = run("match (m:Movie)-[:DIRECTED]-(d:Person) return m, d")
            while (statementResult.hasNext()) {
                val record = statementResult.next()
                LOG.info { record.fields() }
                LOG.info { record.get("m")::class.qualifiedName } // org.neo4j.driver.internal.value.NodeValue
                LOG.info { record.get("m").asObject()::class.qualifiedName } // org.neo4j.driver.internal.InternalNode
                Neo4jBoltRecord(record).run {
                    LOG.info { "Neo4jBoltRecord" }
                    LOG.info { this["m"]!!::class.qualifiedName }
                    LOG.info { this[0]!!::class.qualifiedName }
                    LOG.info { this.asMap() }
                    LOG.info { this.fields() }
                    LOG.info { this.index("m") }
                    LOG.info { this.keys() }
                    LOG.info { this.size() }
                    LOG.info { this.values() }
                    LOG.info { "--------------" }
                }
                val movie = (record["m"] as Value).asNode()
                val director = (record["d"] as Value).asNode()
                LOG.info { "movieId=${movie.id()} directorId=${director.id()}" }
                LOG.info { "movie.labels=${movie.labels()} director.labels=${director.labels()}" }

                if (!mapDirectors.containsKey(director.id().toString()))
                    mapDirectors[director.id().toString()] = PersonResult(director.id(),
                            (director.get("name")!!).asString(),
                            (director.get("born")!!).asLong())

                if (!mapMovies.containsKey(movie.id().toString()))
                    mapMovies[movie.id().toString()] = MovieResult(movie.id(),
                            (movie.get("title")!!).asString(),
                            (movie.get("released")!!).asLong(),
                            mutableListOf())

                mapMovies[movie.id().toString()]?.directors?.add(mapDirectors[director.id().toString()]!!)
            }
        }

        mapMovies.forEach { LOG.info { it } }
    }

    fun shutdown() {
        driver.closeAsync()
        LOG.info { "Closed Neo4j session" }
    }
}


fun main(args: Array<String>) {

    val neo4j = Neo4jBoltTest(neo4jUri = args[0], username = args[1], password = args[2])
    neo4j.sampleObjectQuery()
    neo4j.shutdown()
}

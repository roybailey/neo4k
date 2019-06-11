package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


abstract class BaseNeo4jServiceTest : BaseTest() {

    protected val neo4jService = createNeo4jService()

    protected open fun createNeo4jService(): Neo4jService {
        TODO("Construct your version of Neo4jService in the derived class")
    }

    @BeforeEach
    fun setupDatabase() {
        deleteAllData()
    }

    @AfterEach
    fun shutdownDatabase() {
        //deleteAllData()
        neo4jService.shutdown()
    }

    private fun deleteAllData() {

        neo4jService.execute(Neo4jCypher.deleteAllData())
        val count: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after purge = $count" }
        org.junit.jupiter.api.Assertions.assertEquals(0, count)
    }

    private fun loadMovieData() {

        val countBefore: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count before movies loaded = $countBefore" }

        val cypher = BaseNeo4jServiceTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            LOG.info { "Loaded Movie Graph Data" }
        }

        val countAfter: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after movies loaded = $countAfter" }
        org.junit.jupiter.api.Assertions.assertTrue(countAfter > countBefore)

    }

    @Test
    fun `should load movies graph without errors`() {

        loadMovieData()

        val movieCount: Long = neo4jService.queryForObject("match (m:Movie) return count(m) as movieCount")!!
        org.junit.jupiter.api.Assertions.assertTrue(movieCount > 0)

        val movieData: List<Map<String, Any>> = neo4jService.query("match (m:Movie)--(p:Person) return m,p")
        LOG.info { movieData }
        org.junit.jupiter.api.Assertions.assertTrue(movieData.isNotEmpty())
    }

    @Test
    fun `should query movie graph results`() {

        loadMovieData()

        val query = Neo4jCypher.toNeo4j("""
                match (m:Movie)
                optional match (m)-[:ACTED_IN]-(actor:Person)
                return m.title as title, m.released as released, collect(actor.name) as actors
        """.trimIndent())

        data class MovieResult(val title: String, val released: Long, val actors: List<String>)
        val records = mutableListOf<MovieResult>()

        neo4jService.execute(query, mapOf(Pair("name", "Kevin Bacon"))) { result ->
            while (result.hasNext()) {
                result.next()
                        .also { LOG.info { it } }
                        .also {
                            records.add(MovieResult(
                                    it["title"].toString(),
                                    it["released"] as Long,
                                    it["actors"] as List<String>))
                        }
            }
        }
        records.forEach { LOG.info { it } }
        Assertions.assertThat(records.size).isEqualTo(38)
    }

    @Test
    fun `should query movie graph objects`() {

        loadMovieData()

        val query = "match (m:Movie)-[:DIRECTED]-(d:Person) return m, d"

        data class PersonResult(val name: String, val born: Long)
        data class MovieResult(val title: String, val released: Long, val actors: List<PersonResult>, val directors: List<PersonResult>)
        val records = mutableListOf<MovieResult>()

        neo4jService.execute(query) { result ->
            while (result.hasNext()) {
                result.next()
                        .also { LOG.info { it } }
                        .also {

                            val actors = mutableListOf<PersonResult>()
                            val directors = mutableListOf<PersonResult>()

                            val actor = PersonResult(it["name"].toString(), it["born"] as Long)
                            actors.add(actor)

                            val director = PersonResult(it["name"].toString(), it["born"] as Long)
                            directors.add(director)

                            val movie = MovieResult(it["title"].toString(), it["released"] as Long, actors, directors)
                            records.add(movie)
                        }
            }
            Assertions.assertThat(records.size).isEqualTo(24)
        }
    }
}

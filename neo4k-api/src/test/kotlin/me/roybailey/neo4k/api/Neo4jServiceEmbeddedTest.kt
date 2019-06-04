package me.roybailey.neo4k.api

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class Neo4jServiceEmbeddedTest : BaseTest() {

    private val neo4jService: Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "file://./target/neo4j/Neo4jServiceEmbeddedTest-{timestamp}",
                    boltPort = 7987
            ))

    @AfterEach
    fun deleteAllData() {

        neo4jService.execute(Neo4jCypher.deleteAllData())
        val count: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after purge = $count" }
        assertEquals(0, count)
    }

    @Test
    fun `should load movies graph without errors`() {

        val countBefore: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count before bootstrap = $countBefore" }

        val cypher = Neo4jServiceEmbeddedTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            LOG.info { "Loaded Movie Graph Data" }
        }

        val countAfter: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after bootstrap = $countAfter" }
        assertTrue(countAfter > countBefore)

        val movieCount:Long = neo4jService.queryForObject("match (m:Movie) return count(m) as movieCount")!!
        assertTrue(movieCount > 0)

        val movieData:List<Map<String,Any>> = neo4jService.query("match (m:Movie)--(p:Person) return m,p")
        LOG.info { movieData }
        assertTrue(movieData.isNotEmpty())

    }

}

package me.roybailey.neo4k.api

import mu.KotlinLogging
import org.assertj.core.api.Assertions


class Neo4jTestQueries(val neo4jService: Neo4jService) {

    val LOG = KotlinLogging.logger {}

    fun deleteAllData() {

        neo4jService.execute(Neo4jCypher.deleteAllData())
        val count: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after purge = $count" }
        org.junit.jupiter.api.Assertions.assertEquals(0, count)
    }

    fun loadMovieData() {

        val countBefore: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count before movies loaded = $countBefore" }

        val cypher = BaseNeo4jServiceTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            LOG.info { "Loaded Movie Graph Data" }
        }

        val countAfter: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after movies loaded = $countAfter" }
        Assertions.assertThat(countAfter).isGreaterThan(countBefore)

    }

}


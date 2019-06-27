package me.roybailey.neo4k.springboot.config

import me.roybailey.neo4k.api.BaseNeo4jServiceTest
import me.roybailey.neo4k.api.Neo4jService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant


@Component
class Neo4jBootstrap(
        private val neo4jService: Neo4jService
) {

    private val LOG = KotlinLogging.logger {}

    fun initializeGraph() {

        val initializeTime = Instant.now()

        val cypher = BaseNeo4jServiceTest::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            LOG.info { "Loaded Movie Graph Data" }
        }

        val count: Long? = neo4jService.queryForObject("match (n) return count(n)", mutableMapOf())
        LOG.info { "Loaded Graph Data $count in ${(Instant.now().toEpochMilli() - initializeTime.toEpochMilli())} millis" }
    }
}

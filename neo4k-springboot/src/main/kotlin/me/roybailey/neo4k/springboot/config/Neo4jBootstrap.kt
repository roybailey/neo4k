package me.roybailey.neo4k.springboot.config

import me.roybailey.neo4k.api.Neo4jService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant


@Component
class Neo4jBootstrap(
        private val neo4jService: Neo4jService
) {

    private val logger = KotlinLogging.logger {}

    fun initializeGraph() {

        val initializeTime = Instant.now()

        val cypher = Neo4jService::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            logger.info { "Loaded Movie Graph Data" }
        }

        val count: Long? = neo4jService.queryForObject("match (n) return count(n)", mutableMapOf())
        logger.info { "Loaded Graph Data $count in ${(Instant.now().toEpochMilli() - initializeTime.toEpochMilli())} millis" }
    }
}

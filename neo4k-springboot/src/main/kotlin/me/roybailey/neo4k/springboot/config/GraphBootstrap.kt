package me.roybailey.neo4k.springboot.config

import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.QueryStatement
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant


@Component
class GraphBootstrap(
        val session: Neo4jService
) {

    private val LOG = KotlinLogging.logger {}

    fun initializeGraph() {

        val initializeTime = Instant.now()

        val script = GraphBootstrap::class.java.getResource("/cypher/create-movies.cypher").readText()
        val cypher = QueryStatement.parseQueryScriptStatements(script)[0].query
        session.execute(cypher, mutableMapOf()) {
            LOG.info { it }
        }

        val count: Long? = session.queryForObject("match (n) return count(n)", mutableMapOf())
        LOG.info { "Loaded Graph Data $count in ${(Instant.now().toEpochMilli() - initializeTime.toEpochMilli())} millis" }
        println("Loaded Graph Data $count")
    }
}

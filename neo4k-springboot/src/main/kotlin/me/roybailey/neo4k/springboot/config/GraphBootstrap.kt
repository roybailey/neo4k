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

        val now = Instant.now()

        val cypher = QueryStatement.loadStatements("/cypher/create-movies.cypher")[0].statement
        session.execute(cypher, mutableMapOf()) {
            LOG.info { it }
        }

        val count:Long? = session.queryForObject("match (n) return count(n)", mutableMapOf())
        println("Loaded Graph Data $count")
    }
}

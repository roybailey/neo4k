package me.roybailey.neo4k.springboot.config

import me.roybailey.neo4k.api.Neo4jCypher
import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceOptions
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class GraphConfiguration(val customVariables: Map<String,String> = emptyMap()) {

    val LOG = KotlinLogging.logger {}

    @Value("\${neo4j.bolt.connector.port:0}")
    var neo4jBoltConnectorPort: Int = 0

    @Value("\${neo4j.uri}")
    lateinit var neo4jUri: String

    @Value("\${neo4j.reset:keep}")
    lateinit var neo4jReset: String

    @Bean
    open fun neo4jService(): Neo4jService {
        // initialize embedded Neo4j database
        val neo4jService = Neo4jService.getInstance(
                Neo4jServiceOptions(neo4jUri = neo4jUri, boltPort = neo4jBoltConnectorPort))
        // set static global variables such as sensitive connection values...
        customVariables.forEach {
            neo4jService.setStatic(it.key, it.value) {
                LOG.info { "Assigned Static Variable $it" }
            }
        }
        if("purge".equals(neo4jReset, true)) {
            neo4jService.execute(Neo4jCypher.deleteAllData(), emptyMap()) {
                LOG.info { "NEO4J DATABASE PURGED" }
            }
        } else {
            LOG.info { "NEO4J DATABASE KEPT" }
        }
        return neo4jService
    }

}

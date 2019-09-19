package me.roybailey.neo4k.springboot.config

import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceOptions
import me.roybailey.neo4k.dsl.CypherDsl.cypherMatchAndDeleteAll
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class Neo4jConfiguration(val customVariables: Map<String, String> = emptyMap()) {

    val logger = KotlinLogging.logger {}

    @Value("\${neo4j.bolt.connector.port:0}")
    var neo4jBoltConnectorPort: Int = 0

    @Value("\${neo4j.uri}")
    lateinit var neo4jUri: String

    @Value("\${neo4j.username:neo4j}")
    lateinit var neo4jUsername: String

    @Value("\${neo4j.password:neo4j}")
    lateinit var neo4jPassword: String

    @Value("\${neo4j.reset:keep}")
    lateinit var neo4jReset: String

    @Bean
    open fun neo4jService(): Neo4jService {
        // initialize embedded Neo4j database
        val neo4jService = Neo4jService.getInstance(
                Neo4jServiceOptions(
                        neo4jUri = neo4jUri,
                        boltPort = neo4jBoltConnectorPort,
                        username = neo4jUsername,
                        password = neo4jPassword
                ))
        // set static global variables such as sensitive connection values...
        customVariables.forEach {
            neo4jService.setStatic(it.key, it.value)
            val savedValue = neo4jService.getStatic(it.key)
            if (it.value != savedValue)
                logger.error { "Failed to save apoc static value ${it.key} as ${it.value}" }
        }
        if ("purge".equals(neo4jReset, true)) {
            neo4jService.execute(cypherMatchAndDeleteAll(), emptyMap()) {
                logger.info { "NEO4J DATABASE PURGED" }
            }
        } else {
            logger.info { "NEO4J DATABASE KEPT" }
        }
        return neo4jService
    }

}

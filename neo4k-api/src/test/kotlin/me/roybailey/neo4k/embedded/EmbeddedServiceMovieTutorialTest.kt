package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.neo4j.driver.v1.Record


class EmbeddedServiceMovieTutorialTest : BaseNeo4jServiceMovieTutorialTest() {

    override fun createNeo4jService(): Neo4jService =
        Neo4jService.getInstance(
                Neo4jServiceOptions(
                        neo4jUri = "file://./target/neo4j/EmbeddedServiceQueryApiTest-{timestamp}",
                        boltPort = 7988
                ))

}

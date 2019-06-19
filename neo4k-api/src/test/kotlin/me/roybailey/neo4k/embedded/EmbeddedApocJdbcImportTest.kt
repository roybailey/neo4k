package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.BaseApocJdbcImportTest
import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceOptions


class EmbeddedApocJdbcImportTest : BaseApocJdbcImportTest() {

    override fun createNeo4jService(): Neo4jService =
        Neo4jService.getInstance(
                Neo4jServiceOptions(
                        neo4jUri = "file://./target/neo4j/EmbeddedServiceBasicTest-{timestamp}",
                        boltPort = 7988
                ))

}

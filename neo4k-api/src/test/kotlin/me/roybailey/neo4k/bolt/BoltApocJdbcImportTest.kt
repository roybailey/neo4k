package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.BaseApocJdbcImportTest
import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceOptions


class BoltApocJdbcImportTest : BaseApocJdbcImportTest() {

    override fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "bolt://localhost",
                    boltPort = 7987,
                    username = "neo4j",
                    password = "localhost"
            ))

}

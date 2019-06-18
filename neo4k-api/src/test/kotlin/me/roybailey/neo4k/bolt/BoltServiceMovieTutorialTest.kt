package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class BoltServiceMovieTutorialTest : BaseNeo4jServiceMovieTutorialTest() {

    override fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "bolt://localhost",
                    boltPort = 7987,
                    username = "neo4j",
                    password = "localhost"
            ))

}

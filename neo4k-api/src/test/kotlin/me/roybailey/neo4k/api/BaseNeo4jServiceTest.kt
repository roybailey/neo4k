package me.roybailey.neo4k.api

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo


abstract class BaseNeo4jServiceTest(
        open val neo4jService: Neo4jService,
        val testQueries: Neo4jTestQueries = Neo4jTestQueries(neo4jService)
) : BaseTest() {


    @BeforeEach
    fun setupDatabase(testInfo: TestInfo) {
        testQueries.deleteAllData()
    }

    @AfterEach
    fun shutdownDatabase(testInfo: TestInfo) {
        neo4jService.shutdown()
    }

}

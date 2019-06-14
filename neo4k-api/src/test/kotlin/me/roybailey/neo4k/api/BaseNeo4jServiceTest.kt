package me.roybailey.neo4k.api

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo


abstract class BaseNeo4jServiceTest : BaseTest() {

    protected val neo4jService = createNeo4jService()

    protected val testQueries = Neo4jTestQueries(neo4jService)

    protected open fun createNeo4jService(): Neo4jService {
        TODO("Construct your version of Neo4jService in the derived class")
    }

    @BeforeEach
    fun setupDatabase(testInfo: TestInfo) {
        testQueries.deleteAllData()
    }

    @AfterEach
    fun shutdownDatabase(testInfo: TestInfo) {
        neo4jService.shutdown()
    }

}

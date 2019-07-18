package me.roybailey.neo4k

import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jTestQueries
import me.roybailey.neo4k.testdata.UnitTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo


abstract class Neo4jServiceTestBase(
        open val neo4jService: Neo4jService,
        val testQueries: Neo4jTestQueries = Neo4jTestQueries(neo4jService)
) : UnitTestBase() {


    @BeforeEach
    fun setupDatabase(testInfo: TestInfo) {
        testQueries.deleteAllData()
    }

    @AfterEach
    fun shutdownDatabase(testInfo: TestInfo) {
        neo4jService.shutdown()
    }

}

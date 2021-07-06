package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*
import me.roybailey.neo4k.testdata.UnitTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo


object EmbeddedNeo4jServiceFactory {

    fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "file://./target/neo4j/EmbeddedServiceTest-{timestamp}",
                    boltPort = 7988
            ))
}


abstract class EmbeddedNeo4jServiceTestBase : UnitTestBase() {

    val neo4jImportFolder: String = projectTestDataFolder
    val apiTestServer: String = "localhost"

    var neo4jService: Neo4jService = EmbeddedNeo4jServiceFactory.createNeo4jService()
    var testQueries: Neo4jTestQueries = Neo4jTestQueries(neo4jService)

    @BeforeEach
    fun setupDatabase(testInfo: TestInfo) {
        testQueries.deleteAllData()
    }

    @AfterEach
    fun shutdownDatabase(testInfo: TestInfo) {
        neo4jService.shutdown()
    }
}


class EmbeddedServiceBasicTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceBasicTestSuite


class EmbeddedServiceMovieTutorialTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceMovieTutorialTestSuite


class EmbeddedServiceQueryApiTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceQueryApiTestSuite


class EmbeddedServiceApocTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceApocTestSuite


class EmbeddedServiceLoadCsvTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceLoadCsvTestSuite


class EmbeddedServiceApocLoadJdbcTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceApocLoadJdbcTestSuite


class EmbeddedServiceApocLoadJsonTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceApocLoadJsonTestSuite


class EmbeddedServiceNorthwindScriptTest : EmbeddedNeo4jServiceTestBase(), Neo4jServiceNorthwindScriptTestSuite

package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.*
import me.roybailey.neo4k.server.TestApiServer
import me.roybailey.neo4k.testdata.UnitTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.MountableFile


class KNeo4jContainer : Neo4jContainer<KNeo4jContainer>()

object BoltNeo4jServiceFactory {

    fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
        Neo4jServiceOptions(
            neo4jUri = "bolt://localhost",
            boltPort = 7987,
            username = "neo4j",
            password = "localhost"
        )
    )

}


@Testcontainers
abstract class BoltNeo4jServiceTestContainerBase : UnitTestBase() {

    companion object {

        val testApiServer = TestApiServer.createTestApiServer()

        // this will expose the local test server ports to the test container, so it can call out to host
        // needed to allow the neo4j test container to call our test api server instances
        val mapLocalTestApiServer = org.testcontainers.Testcontainers.exposeHostPorts(
            *TestApiServer.getTestApiServers().map { it.port }.toIntArray()
        )

        @JvmStatic
        @Container
        val neo4jContainer = KNeo4jContainer()
            .withAdminPassword("localhost")
            .withPlugins(MountableFile.forClasspathResource("/neo4j/plugins"))
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*")
            .withNeo4jConfig("apoc.import.file.enabled", "true")
            .withNeo4jConfig("dbms.directories.import", "/")
            .withFileSystemBind(
                "$projectTestDataFolder", "/var/lib/neo4j/import",
                BindMode.READ_ONLY
            )
        //.withNetwork(Network.newNetwork())
        //.withExtraHost("host.testcontainers.localhost", InetAddress.getLocalHost().hostAddress)
    }

    val neo4jImportFolder: String = "/var/lib/neo4j/import"
    val apiTestServer: String = "host.testcontainers.internal"

    var neo4jService: Neo4jService = Neo4jService.getInstance(
        Neo4jServiceOptions(
            neo4jUri = neo4jContainer.boltUrl,
            boltPort = 0,
            username = "neo4j",
            password = "localhost"
        )
    )
    var testQueries = Neo4jTestQueries(neo4jService)


    @BeforeEach
    fun setupDatabase(testInfo: TestInfo) {
        testQueries = Neo4jTestQueries(neo4jService)
        testQueries.deleteAllData()
    }

    @AfterEach
    fun shutdownDatabase(testInfo: TestInfo) {
        neo4jService.shutdown()
    }

}


class BoltServiceBasicTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceBasicTestSuite


class BoltServiceMovieTutorialTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceMovieTutorialTestSuite


class BoltServiceQueryApiTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceQueryApiTestSuite


class BoltServiceApocTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceApocTestSuite


class BoltServiceLoadCsvTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceLoadCsvTestSuite


class BoltServiceApocLoadJdbcTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceApocLoadJdbcTestSuite


class BoltServiceApocLoadJsonTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceApocLoadJsonTestSuite


class BoltServiceNorthwindScriptTest : BoltNeo4jServiceTestContainerBase(), Neo4jServiceNorthwindScriptTestSuite

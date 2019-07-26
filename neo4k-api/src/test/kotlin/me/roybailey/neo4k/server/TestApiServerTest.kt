package me.roybailey.neo4k.server

import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.JSON_100_TESTDATA
import me.roybailey.neo4k.testdata.UnitTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.net.URL


class TestApiServerTest : UnitTestBase() {


    val api = TestApiServer.createTestApiServer()


    @BeforeEach
    fun startWebApiServer() {
        val data = FileReader("$projectTestDataFolder/$JSON_100_TESTDATA").readText()
        api.start(data)
    }


    @AfterEach
    fun stopWebApiServer() {
        api.stop()
    }


    @Test
    fun testApiServer() {

        val response = try {
            URL("${api.url}/testdata")
                    .openStream()
                    .bufferedReader()
                    .use {
                        val data = it.readText()
                        logger.info { data }
                    }
        } catch (err: Exception) {
            logger.error { err }
            throw RuntimeException("Local test api server not working!!", err)
        }
    }
}
package me.roybailey.neo4k.api

import com.fasterxml.jackson.databind.ObjectMapper
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.JSON_TESTDATA_MERGE
import me.roybailey.neo4k.dsl.CypherDsl.apocLoadJson
import me.roybailey.neo4k.dsl.CypherDsl.apocPeriodicIterate
import me.roybailey.neo4k.dsl.escapeDoubleQuotes
import me.roybailey.neo4k.dsl.quoted
import me.roybailey.neo4k.dsl.singleQuoted
import me.roybailey.neo4k.server.TestApiServer
import me.roybailey.neo4k.testdata.UnitTestBase.Companion.projectTestDataFolder
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.time.Duration


interface Neo4jServiceApocLoadJsonTestSuite : Neo4jServiceTestSuiteBase {

    companion object {

        private val testApi =
            TestApiServer.createTestApiServer()
    }

    // use the override server address to ensure docker containers can access the api
    private val resolvedTestApiServerUrl: String
        get() = "http://" + apiTestServer + ":" + testApi.port

    @BeforeEach
    fun startWebApiServer() {
        val data = ObjectMapper().readValue(
            FileReader("$projectTestDataFolder/${Neo4jTestQueries.JSON_100_TESTDATA}").readText(),
            mutableMapOf<Any, Any>()::class.java
        )
        testApi.start(data)
        logger.info { "Started ApiServer on ${testApi.url}" }
    }

    @AfterEach
    fun stopWebApiServer() {
        logger.info { "Stopping ApiServer on ${testApi.url}" }
        testApi.stop()
    }


    /**
     * Tests the direct Neo4jService query execution which returns Neo4j Result object
     */
    @Test
    fun `test apocLoadJson using cypher`() {

        val url = "$resolvedTestApiServerUrl/testdata"
        val cypher = """
            CALL apoc.load.jsonParams("$url",{},null) YIELD value WITH value
            UNWIND value.listSuppliers AS supplier
            $JSON_TESTDATA_MERGE
            """.trimIndent()

        logger.info { "Running append:\n\n$cypher\n\n" }

        val results = mutableListOf<Map<String, Any>>()
        neo4jService.execute(cypher, emptyMap()) { rs ->

            logger.info { rs.keys() }

            while (rs.hasNext()) {
                results.add(rs.next().asMap())
            }
        }

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!

        SoftAssertions().apply {
            assertThat(totalSuppliers).isEqualTo(10L)
            assertThat(totalProducts).isEqualTo(1000L)
        }.assertAll()
    }


    @Test
    fun `test apocLoadJson using cypher DSL`() {

        val cypher = apocLoadJson {
            url = "$resolvedTestApiServerUrl/testdata".quoted()
            cypher = """
                UNWIND value.listSuppliers AS supplier
                $JSON_TESTDATA_MERGE
                """.trimIndent()
        }

        logger.info { "Running append:\n\n$cypher\n\n" }

        val results = mutableListOf<Map<String, Any>>()
        neo4jService.execute(cypher, emptyMap()) { rs ->

            logger.info { rs.keys() }

            while (rs.hasNext()) {
                results.add(rs.next().asMap())
            }
        }

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!

        SoftAssertions().apply {
            assertThat(totalSuppliers).isEqualTo(10L)
            assertThat(totalProducts).isEqualTo(1000L)
        }.assertAll()
    }


    @Test
    fun `test apocPeriodicIterate and apocLoadJson using cypher DSL`() {

        val cypher = apocPeriodicIterate {
            outer = apocLoadJson {
                url = "$resolvedTestApiServerUrl/testdata".singleQuoted()
                with = "RETURN value"
            }.escapeDoubleQuotes()
            inner = """
                    UNWIND value.listSuppliers AS supplier
                    $JSON_TESTDATA_MERGE
                    """.trimIndent()
            batchSize = 10
        }

        logger.info { "Running append:\n\n$cypher\n\n" }

        val expectedRecords = 1L

        val result = Assertions.assertTimeoutPreemptively<Map<String, Any>>(Duration.ofMinutes(3)) {
            var result = emptyMap<String, Any>()
            neo4jService.execute(cypher, emptyMap()) { rs ->
                result = rs.single().asMap()
            }
            result
        }
        logger.info { result }

        SoftAssertions().apply {
            assertThat(result["total"]).isEqualTo(expectedRecords)
            assertThat(result["batches"]).isEqualTo(1L)
            assertThat(result["failedBatches"]).isEqualTo(0L)
        }.assertAll()

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!

        SoftAssertions().apply {
            assertThat(totalSuppliers).isEqualTo(10L)
            assertThat(totalProducts).isEqualTo(1000L)
        }.assertAll()
    }
}



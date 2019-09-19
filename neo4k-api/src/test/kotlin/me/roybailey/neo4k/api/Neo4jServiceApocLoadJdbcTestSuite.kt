package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_1000_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_100_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_APOC
import me.roybailey.neo4k.dsl.CypherDsl.apocLoadJdbc
import me.roybailey.neo4k.dsl.CypherDsl.apocPeriodicIterate
import me.roybailey.neo4k.dsl.escapeDoubleQuotes
import me.roybailey.neo4k.dsl.quoted
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.time.Duration.ofMinutes


interface Neo4jServiceApocLoadJdbcTestSuite : Neo4jServiceTestSuiteBase {


    @Test
    fun `test apocLoadJdbc using cypher`() {

        val url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
        val cypher = """
            CALL apoc.load.jdbc("$url",
                "SELECT * FROM CSVREAD('$neo4jImportFolder/$CSV_100_TESTDATA')"
            ) YIELD row WITH row
            $CSV_TESTDATA_MERGE_APOC
          """.trimIndent()

        logger.info { "Running append:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap()) { rs ->
            assertThat(rs.single()["totalProducts"]).isEqualTo(100L)
        }

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!

        SoftAssertions().apply {
            assertThat(totalProducts).isEqualTo(100L)
            assertThat(totalSuppliers).isEqualTo(10L)
        }.assertAll()
    }


    @Test
    fun `test apocLoadJdbc using cypher DSL`() {

        val cypher = apocLoadJdbc {
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1".quoted()
            select = "SELECT * FROM CSVREAD('$neo4jImportFolder/$CSV_100_TESTDATA')"
            cypher = CSV_TESTDATA_MERGE_APOC
        }

        logger.info { "Running append:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap()) { rs ->
            assertThat(rs.single()["totalProducts"]).isEqualTo(100L)
        }

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!

        SoftAssertions().apply {
            assertThat(totalProducts).isEqualTo(100L)
            assertThat(totalSuppliers).isEqualTo(10L)
        }.assertAll()
    }


    @Test
    fun `test apocPeriodicIterate and apocLoadJdbc using cypher DSL`() {

        val cypher = apocPeriodicIterate {
            outer = apocLoadJdbc {
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1".quoted(doubleQuotes = false)
                select = "SELECT * FROM CSVREAD('$neo4jImportFolder/$CSV_1000_TESTDATA')"
                with = "RETURN row"
            }.escapeDoubleQuotes()
            inner = CSV_TESTDATA_MERGE_APOC
            batchSize = 100
        }

        logger.info { "Running cypher:\n\n$cypher\n\n" }

        val expectedRecords = 1000L
        val expectedProducts = 1000L
        val expectedSuppliers = 100L

        val result = assertTimeoutPreemptively<Map<String, Any>>(ofMinutes(3)) {
            var result = emptyMap<String, Any>()
            neo4jService.execute(cypher, emptyMap()) { rs ->
                result = rs.single().asMap()
            }
            result
        }
        logger.info { result }
        assertThat(result["total"]).isEqualTo(expectedRecords)
        assertThat(result["batches"]).isEqualTo(10L)
        assertThat(result["failedBatches"]).isEqualTo(0L)

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!

        SoftAssertions().apply {
            assertThat(totalProducts).isEqualTo(expectedProducts)
            assertThat(totalSuppliers).isEqualTo(expectedSuppliers)
        }.assertAll()
    }

}



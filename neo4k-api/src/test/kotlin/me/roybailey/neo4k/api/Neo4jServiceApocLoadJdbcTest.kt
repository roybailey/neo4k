package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_1000_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_100_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_APOC
import me.roybailey.neo4k.api.ScriptDsl.apocLoadJdbc
import me.roybailey.neo4k.api.ScriptDsl.apocPeriodicIterate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.time.Duration.ofMinutes


abstract class Neo4jServiceApocLoadJdbcTest(final override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {

    @Test
    fun `test apocLoadJdbc using cypher`() {

        val cypher = Neo4jApoc.apocLoadJdbc(
                dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                sql = "SELECT * FROM CSVREAD('$testDataFolder/$CSV_100_TESTDATA')",
                process = CSV_TESTDATA_MERGE_APOC
        )

        logger.info { "Running append:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap()) { rs ->
            assertThat(rs.single()["totalProducts"]).isEqualTo(100L)
        }

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        assertThat(totalProducts).isEqualTo(12L)

        val totalCountries = neo4jService.queryForObject<Long>("match (c:Country) return count(c) as totalCountries")!!
        assertThat(totalCountries).isEqualTo(76L)

    }


    @Test
    fun `test apocLoadJdbcBatch using cypher`() {

        val cypher = Neo4jApoc.apocLoadJdbcBatch(
                dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                useStaticDbUrl = false,
                sql = "SELECT * FROM CSVREAD('$testDataFolder/$CSV_1000_TESTDATA')",
                process = CSV_TESTDATA_MERGE_APOC,
                batchsize = 100
        )

        logger.info { "Running append:\n\n$cypher\n\n" }

        val expectedRecords = 1000L
        val expectedProducts = 12L
        val expectedCountries = 185L

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

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        assertThat(totalProducts).isEqualTo(expectedProducts)

        logger.info { neo4jService.query("match (c:Country) return c.country as country") { it.asMap() } }

        val totalCountries = neo4jService.queryForObject<Long>("match (c:Country) return count(c) as totalCountries")!!
        assertThat(totalCountries).isEqualTo(expectedCountries)
    }


    @Test
    fun `test apocLoadJdbc using cypher DSL`() {

        val cypher = apocLoadJdbc {
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1".quoted()
            select = "SELECT * FROM CSVREAD('$testDataFolder/$CSV_100_TESTDATA')"
            cypher = CSV_TESTDATA_MERGE_APOC
        }

        logger.info { "Running append:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap()) { rs ->
            assertThat(rs.single()["totalProducts"]).isEqualTo(100L)
        }

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        assertThat(totalProducts).isEqualTo(12L)

        val totalCountries = neo4jService.queryForObject<Long>("match (c:Country) return count(c) as totalCountries")!!
        assertThat(totalCountries).isEqualTo(76L)

    }


    @Test
    fun `test apocPeriodicIterate and apocLoadJdbc using cypher DSL`() {

        val cypher = apocPeriodicIterate {
            outer = apocLoadJdbc {
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1".quoted(doubleQuotes = false)
                select = "SELECT * FROM CSVREAD('$testDataFolder/$CSV_1000_TESTDATA')"
                with = "RETURN row"
            }.escapeDoubleQuotes()
            inner = CSV_TESTDATA_MERGE_APOC
            batchSize = 100
        }

        logger.info { "Running append:\n\n$cypher\n\n" }

        val expectedRecords = 1000L
        val expectedProducts = 12L
        val expectedCountries = 185L

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

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        assertThat(totalProducts).isEqualTo(expectedProducts)

        logger.info { neo4jService.query("match (c:Country) return c.country as country") { it.asMap() } }

        val totalCountries = neo4jService.queryForObject<Long>("match (c:Country) return count(c) as totalCountries")!!
        assertThat(totalCountries).isEqualTo(expectedCountries)
    }

}



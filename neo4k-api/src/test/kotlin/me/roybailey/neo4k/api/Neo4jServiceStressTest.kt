package me.roybailey.neo4k.api

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import java.time.Duration
import java.time.Instant.now


/**
 * Test application to load really large datasets into Neo4j to check performance.
 * Not built as a test since it would materially impact the build cycle.
 */
class Neo4jServiceStressTest(val neo4jService: Neo4jService) {

    val LOG = KotlinLogging.logger {}


    fun neo4jLoadCsvBatch(csvFilename: String, process: String) {
        val cypher = Neo4jApoc.apocLoadJdbcBatch(
                dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                useStaticDbUrl = false,
                sql = "SELECT * FROM CSVREAD('$csvFilename')",
                process = process
        )

        LOG.info { "Loading file: $csvFilename" }
        LOG.info { "Running cypher:\n\n$cypher\n\n" }

        val expectedRecords = 50000L
        val expectedProducts = 12L
        val expectedCountries = 185L
        val started = now()

        var result = emptyMap<String, Any>()
        neo4jService.execute(cypher, emptyMap()) { rs ->
            result = rs.single().asMap()
        }
        LOG.info { result }
        LOG.info { "Completed bulk load in ${(now().toEpochMilli() - started.toEpochMilli()) / 60000F} minutes" }
        org.assertj.core.api.Assertions.assertThat(result["total"]).isEqualTo(expectedRecords)
        org.assertj.core.api.Assertions.assertThat(result["failedBatches"]).isEqualTo(0L)

        neo4jService.execute("match (p:Product) return count(p) as totalProducts") {
            it.single()["totalProducts"].let { totalProducts ->
                LOG.info { "Found $totalProducts products" }
                org.assertj.core.api.Assertions.assertThat(totalProducts).isEqualTo(expectedProducts)
            }
        }

        neo4jService.execute("match (c:Country) return count(c) as totalCountries") {
            it.single()["totalCountries"].let { totalCountries ->
                LOG.info { "Found $totalCountries countrues" }
                org.assertj.core.api.Assertions.assertThat(totalCountries).isEqualTo(expectedCountries)
            }
        }
    }


    fun shutdown() {
        neo4jService.shutdown()
    }
}

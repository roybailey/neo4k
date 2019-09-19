package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_10000_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_50000_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_APOC
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.findTestDataFile
import me.roybailey.neo4k.dsl.CypherDsl.apocLoadJdbc
import me.roybailey.neo4k.dsl.CypherDsl.cypherMatchAndDeleteAll
import me.roybailey.neo4k.dsl.quoted
import mu.KotlinLogging
import java.time.Instant.now


/**
 * Test application to load really large datasets into Neo4j to check performance.
 * Not built as a test since it would materially impact the build cycle.
 */
class Neo4jServiceStressTest(val neo4jService: Neo4jService) {

    val logger = KotlinLogging.logger {}


    fun neo4jLoadCsvBatch10000() = neo4jLoadCsvBatch(
            findTestDataFile(CSV_10000_TESTDATA),
            CSV_TESTDATA_MERGE_APOC,
            10000L,
            Pair("match (p:Product) return count(p) as totalProducts", 12L),
            Pair("match (c:Country) return count(c) as totalCountries", 185L)
    )


    fun neo4jLoadCsvBatch50000() = neo4jLoadCsvBatch(
            findTestDataFile(CSV_50000_TESTDATA),
            CSV_TESTDATA_MERGE_APOC,
            50000L,
            Pair("match (p:Product) return count(p) as totalProducts", 12L),
            Pair("match (c:Country) return count(c) as totalCountries", 185L)
    )


    fun neo4jLoadCsvBatch(csvFilename: String,
                          process: String,
                          expectedRecords: Long,
                          vararg checks: Pair<String, Any>
    ) {
        val cypher = apocLoadJdbc {
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1".quoted()
            select = "SELECT * FROM CSVREAD('$csvFilename')"
            cypher = process
        }

        logger.info { "Loading file: $csvFilename" }
        neo4jService.execute(cypherMatchAndDeleteAll())
        logger.info { "Running append:\n\n$cypher\n\n" }

        val started = now()

        var result = emptyMap<String, Any>()
        neo4jService.execute(cypher, emptyMap()) { rs ->
            result = rs.single().asMap()
        }
        logger.info { result }
        logger.info { "Completed bulk load in ${(now().toEpochMilli() - started.toEpochMilli()) / 60000F} minutes" }
        org.assertj.core.api.Assertions.assertThat(result["total"]).isEqualTo(expectedRecords)
        org.assertj.core.api.Assertions.assertThat(result["failedBatches"]).isEqualTo(0L)

        checks.forEach { check ->
            neo4jService.execute(check.first) {
                it.single().asMap().values.first().let { count ->
                    logger.info { "Found $count for ${check.first}" }
                    org.assertj.core.api.Assertions.assertThat(count).isEqualTo(check.second)
                }
            }
        }
    }


    fun shutdown() {
        neo4jService.shutdown()
    }
}

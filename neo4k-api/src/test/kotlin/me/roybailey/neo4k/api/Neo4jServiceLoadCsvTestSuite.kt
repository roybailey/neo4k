package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_100_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_NEO4J
import me.roybailey.neo4k.dsl.ScriptDsl.loadCsvWithHeaders
import me.roybailey.neo4k.testdata.UnitTestBase.Companion.projectTestDataFolder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


interface Neo4jServiceLoadCsvTestSuite : Neo4jServiceTestSuiteBase {

    @Test
    fun `should load csv file without errors`() {

        val csvUrl = "file://$neo4jImportFolder/$CSV_100_TESTDATA"
        val csvUrltest = "file:///$CSV_100_TESTDATA"
        val cypher = loadCsvWithHeaders(fileUrl = csvUrl) {
            cypher = CSV_TESTDATA_MERGE_NEO4J
        }

        logger.info { "Running append:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap())

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        Assertions.assertThat(totalProducts).isEqualTo(100L)

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        Assertions.assertThat(totalSuppliers).isEqualTo(10L)

    }

}

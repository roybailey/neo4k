package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_100_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_NEO4J
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


abstract class Neo4jServiceLoadCsvTest(override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {

    @Test
    fun `should load csv file without errors`() {

        val cypher = Neo4jCypher.loadCsvWithHeaders(
                fileUrl = "file://$projectTestDataFolder/$CSV_100_TESTDATA",
                withLineCypher = CSV_TESTDATA_MERGE_NEO4J)

        logger.info { "Running append:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap())

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        Assertions.assertThat(totalProducts).isEqualTo(100L)

        val totalSuppliers = neo4jService.queryForObject<Long>("match (s:Supplier) return count(s) as totalSuppliers")!!
        Assertions.assertThat(totalSuppliers).isEqualTo(10L)

    }

}

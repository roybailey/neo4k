package me.roybailey.neo4k.api

import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_100_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_NEO4J
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


abstract class Neo4jServiceLoadCsvTest(override val neo4jService: Neo4jService)
    : BaseNeo4jServiceTest(neo4jService) {

    @Test
    fun `should load csv file without errors`() {

        val cypher = Neo4jCypher.loadCsvWithHeaders(
                fileUrl = "file://$testDataFolder/$CSV_100_TESTDATA",
                withLineCypher = CSV_TESTDATA_MERGE_NEO4J)

        LOG.info { "Running cypher:\n\n$cypher\n\n" }

        neo4jService.execute(cypher, emptyMap())

        val totalProducts = neo4jService.queryForObject<Long>("match (p:Product) return count(p) as totalProducts")!!
        Assertions.assertThat(totalProducts).isEqualTo(12L)

        val totalCountries = neo4jService.queryForObject<Long>("match (c:Country) return count(c) as totalCountries")!!
        Assertions.assertThat(totalCountries).isEqualTo(76L)

    }

}

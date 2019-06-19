package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


abstract class BaseServiceApocLoadJdbcTest(final override val neo4jService: Neo4jService)
    : BaseNeo4jServiceTest(neo4jService) {


    private fun cypherSample() = Neo4jApoc.apocLoadJdbc(
            dbUrl = "'jdbc:h2:mem:test;DB_CLOSE_DELAY=-1'",
            sql = "SELECT * FROM CSVREAD('$testDataFolder/SampleCSVFile_2kb.csv')",
            merge = """
                RETURN
                row.PRODUCT as PRODUCT,
                apoc.text.base64Encode(row.FULLNAME) as FULLNAME,
                row.PRICE as PRICE,
                row.UNITPRICE as UNITPRICE,
                apoc.text.toUpperCase(COALESCE(row.CATEGORY, "")) as CATEGORY,
                row.BRAND as BRAND,
                row.QUANTITY as QUANTITY,
                row.DISCOUNT as DISCOUNT
            """.trimIndent()
    )


    /**
     * Tests the direct Neo4jService query execution which returns Neo4j Result object
     */
    @Test
    fun `test JDBC cypher load query`() {

        val cypher = cypherSample()

        LOG.info { "Running cypher:\n\n$cypher\n\n" }

        val results = mutableListOf<Map<String, Any>>()
        neo4jService.execute(cypher, emptyMap()) { rs ->

            LOG.info { rs.keys() }

            while (rs.hasNext()) {
                results.add(rs.next().asMap())
            }
        }
        assertThat(results).hasSize(10)
        results.forEach { LOG.info { it } }
    }

}



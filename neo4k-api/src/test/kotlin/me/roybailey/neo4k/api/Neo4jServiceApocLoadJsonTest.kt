package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import me.roybailey.neo4k.api.ScriptDsl.apocLoadJson
import me.roybailey.neo4k.api.ScriptDsl.quote
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


abstract class Neo4jServiceApocLoadJsonTest(final override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {


    private fun cypherSample() = Neo4jApoc.apocLoadJson(
            url = "https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=creation&tagged=neo4j&site=stackoverflow&filter=!5-i6Zw8Y)4W7vpy91PMYsKM-k9yzEsSC1_Uxlf",
            process = """
                UNWIND value.items AS item
                RETURN item.title, item.owner, item.creation_date, keys(item)
            """.trimIndent()
    )


    /**
     * Tests the direct Neo4jService query execution which returns Neo4j Result object
     */
    @Test
    fun `test cypher load json query`() {

        val cypher = cypherSample()

        logger.info { "Running append:\n\n$cypher\n\n" }

        val results = mutableListOf<Map<String, Any>>()
        neo4jService.execute(cypher, emptyMap()) { rs ->

            logger.info { rs.keys() }

            while (rs.hasNext()) {
                results.add(rs.next().asMap())
            }
        }
        results.forEach { logger.info { it } }
        assertThat(results).hasSize(100)
    }



    /**
     * Tests the direct Neo4jService query execution which returns Neo4j Result object
     */
    @Test
    fun `test cypher DSL load json query`() {

        val cypher = apocLoadJson {
            url = quote("https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=creation&tagged=neo4j&site=stackoverflow&filter=!5-i6Zw8Y)4W7vpy91PMYsKM-k9yzEsSC1_Uxlf")
            cypher = """
                UNWIND value.items AS item
                RETURN item.title, item.owner, item.creation_date, keys(item)
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
        results.forEach { logger.info { it } }
        assertThat(results).hasSize(100)
    }
}



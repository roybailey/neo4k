package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import me.roybailey.neo4k.api.ScriptDsl.apocLoadJson
import me.roybailey.neo4k.api.ScriptDsl.apocPeriodicIterate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Duration


abstract class Neo4jServiceApocLoadJsonTest(final override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {


    /**
     * Tests the direct Neo4jService query execution which returns Neo4j Result object
     */
    @Test
    fun `test apocLoadJson using cypher`() {

        val url = "https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=creation&tagged=neo4j&site=stackoverflow&filter=!5-i6Zw8Y)4W7vpy91PMYsKM-k9yzEsSC1_Uxlf"
        val cypher = """
            CALL apoc.load.jsonParams("$url",{},null) YIELD value WITH value
            UNWIND value.items AS item
            RETURN item.title, item.owner, item.creation_date, keys(item)
            """.trimIndent()

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


    @Test
    fun `test apocLoadJson using cypher DSL`() {

        val cypher = apocLoadJson {
            url = "https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=creation&tagged=neo4j&site=stackoverflow&filter=!5-i6Zw8Y)4W7vpy91PMYsKM-k9yzEsSC1_Uxlf".quoted()
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


    @Test
    fun `test apocPeriodicIterate and apocLoadJson using cypher DSL`() {

        val cypher = apocPeriodicIterate {
            outer = apocLoadJson {
                url = "https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=creation&tagged=neo4j&site=stackoverflow&filter=!5-i6Zw8Y)4W7vpy91PMYsKM-k9yzEsSC1_Uxlf".quoted()
                with = "RETURN value"
            }.escapeDoubleQuotes()
            inner = """
                    UNWIND value.items AS item
                    RETURN item.title, item.owner, item.creation_date, keys(item)
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
        assertThat(result["total"]).isEqualTo(expectedRecords)
        assertThat(result["batches"]).isEqualTo(1L)
        assertThat(result["failedBatches"]).isEqualTo(0L)
    }
}



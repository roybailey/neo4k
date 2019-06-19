package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


abstract class BaseNeo4jServiceLoadCsvTest(override val neo4jService: Neo4jService)
    : BaseNeo4jServiceTest(neo4jService) {

    @Test
    fun `should load csv file without errors`() {

        val cypher = Neo4jCypher.loadCsvWithHeaders(
                fileUrl = "file://$testDataFolder/SampleCSVFile_2kb.csv",
                withLineCypher = """
                        return
                        line.Product as product,
                        apoc.text.base64Encode(line.Fullname) as fullname,
                        line.Price as price,
                        line.UnitPrice as unitPrice,
                        apoc.text.toUpperCase(COALESCE(line.Category, "")) as category,
                        line.Brand as brand,
                        line.Quantity as quantity,
                        line.Discount as discount
                """.trimIndent())

        LOG.info { "Running cypher:\n\n$cypher\n\n" }

        val results = mutableListOf<Map<String, Any>>()
        neo4jService.execute(cypher, emptyMap()) { rs ->

            LOG.info { rs.keys() }

            while (rs.hasNext()) {
                results.add(rs.next().asMap())
            }
        }
        Assertions.assertThat(results).hasSize(10)
        results.forEach { LOG.info { it } }
    }

}

package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.time.Duration.ofMinutes




abstract class Neo4jServiceApocLoadJdbcTest(final override val neo4jService: Neo4jService)
    : BaseNeo4jServiceTest(neo4jService) {


    @Test
    fun `test apoc load JDBC cypher`() {

        val cypher = Neo4jApoc.apocLoadJdbc(
                dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                sql = "SELECT * FROM CSVREAD('$testDataFolder/SampleCSVFile_2kb.csv')",
                process = """
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


    @Test
    fun `test apoc batch load JDBC cypher`() {

        val cypher = Neo4jApoc.apocLoadJdbcBatch(
                dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                useStaticDbUrl = false,
                sql = "SELECT * FROM CSVREAD('$testDataFolder/SampleCSVFile_53000kb.csv')",
                process = """
                    MERGE (p:Product {product: row.PRODUCT})
                      SET p += row,
                          p.fullname = row.FULLNAME,
                          p.price = row.PRICE,
                          p.unitPrice = row.UNITPRICE,
                          p.brand = row.BRAND,
                          p.quantity = row.QUANTITY,
                          p.discount = row.DISCOUNT
                      MERGE (c:Category {name: apoc.text.toUpperCase(COALESCE(row.CATEGORY, ''))})
                      MERGE (p)-[:BELONGS_TO_BY]->(c)
                      RETURN count(p) as totalProducts
            """.trimIndent()
        )

        LOG.info { "Running cypher:\n\n$cypher\n\n" }

        val expectedRecords = 59507L
        val expectedProducts = 1263L
        val expectedCategories = 18L

        val result = assertTimeoutPreemptively<Map<String,Any>>(ofMinutes(3)) {
            var result = emptyMap<String,Any>()
            neo4jService.execute(cypher, emptyMap()) { rs ->
                result = rs.single().asMap()
            }
            result
        }
        LOG.info { result }
        assertThat(result["total"]).isEqualTo(expectedRecords)
        assertThat(result["failedBatches"]).isEqualTo(0L)

        neo4jService.execute("match (p:Product) return count(p) as totalProducts") {
            it.single()["totalProducts"].let { totalProducts ->
                LOG.info { "Found $totalProducts products" }
                assertThat(totalProducts).isEqualTo(expectedProducts)
            }
        }

        neo4jService.execute("match (c:Category) return count(c) as totalCategories") {
            it.single()["totalCategories"].let { totalCategories ->
                LOG.info { "Found $totalCategories categories" }
                assertThat(totalCategories).isEqualTo(expectedCategories)
            }
        }
    }

}



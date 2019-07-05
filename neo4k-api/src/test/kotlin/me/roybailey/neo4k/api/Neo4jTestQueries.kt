package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import java.io.File


class Neo4jTestQueries(val neo4jService: Neo4jService) {

    val LOG = KotlinLogging.logger {}

    fun deleteAllData() {

        neo4jService.execute(Neo4jCypher.deleteAllData())
        val count: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after purge = $count" }
        org.junit.jupiter.api.Assertions.assertEquals(0, count)
    }

    fun loadMovieData() {

        val countBefore: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count before movies loaded = $countBefore" }

        val cypher = Neo4jServiceTestBase::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            LOG.info { "Loaded Movie Graph Data" }
        }

        val countAfter: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        LOG.info { "graph count after movies loaded = $countAfter" }
        Assertions.assertThat(countAfter).isGreaterThan(countBefore)

    }

    companion object {

        val CSV_100_TESTDATA = "100-Sales-Records.csv"
        val CSV_1000_TESTDATA = "1000-Sales-Records.csv"
        val CSV_10000_TESTDATA = "10000-Sales-Records.csv"
        val CSV_50000_TESTDATA = "50000-Sales-Records.csv"

        fun findTestDataFile(filename:String) = File(".").walkTopDown().first { it.name == filename }.absolutePath

        // merge from csv read `row` variable data
        // (note: neo4j converts single word columns to uppercase, while columns with spaces need back-quotes to read)
        // columns from test data file...
        // Region,Country,Item Type,Sales Channel,Order Priority,Order Date,Order ID,Ship Date,Units Sold,Unit Price,Unit Cost,Total Revenue,Total Cost,Total Profit

        val CSV_TESTDATA_MERGE_APOC = """
            MERGE (c:Country {country: apoc.text.toUpperCase(COALESCE(row.COUNTRY, 'unknown'))})
              SET c.region = row.REGION
            MERGE (p:Product {product: row.`Item Type`})
            MERGE (o:Order {orderId: row.`Order ID`})
              SET o.salesChannel = row.`Sales Channel`,
                  o.orderPriority = row.`Order Priority`,
                  o.orderDate = row.`Order Date`,
                  o.shipDate = row.`Ship Date`,
                  o.quantity = row.`Units Sold`,
                  o.unitPrice = row.`Unit Price`
              MERGE (o)-[:FROM]->(c)
              MERGE (o)-[:OF]->(p)
              RETURN count(p) as totalProducts
        """.trimIndent()

        val CSV_TESTDATA_MERGE_NEO4J = """
            MERGE (c:Country {country: apoc.text.toUpperCase(COALESCE(row.Country, 'unknown'))})
              SET c.region = row.Region
            MERGE (p:Product {product: row.`Item Type`})
            MERGE (o:Order {orderId: row.`Order ID`})
              SET o.salesChannel = row.`Sales Channel`,
                  o.orderPriority = row.`Order Priority`,
                  o.orderDate = row.`Order Date`,
                  o.shipDate = row.`Ship Date`,
                  o.quantity = row.`Units Sold`,
                  o.unitPrice = row.`Unit Price`
              MERGE (o)-[:FROM]->(c)
              MERGE (o)-[:OF]->(p)
              RETURN count(p) as totalProducts
        """.trimIndent()
    }

}


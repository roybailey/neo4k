package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import java.io.File


class Neo4jTestQueries(val neo4jService: Neo4jService) {

    val logger = KotlinLogging.logger {}

    fun deleteAllData() {

        neo4jService.execute(me.roybailey.neo4k.dsl.ScriptDsl.cypherMatchAndDeleteAll())
        val count: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        logger.info { "graph count after purge = $count" }
        org.junit.jupiter.api.Assertions.assertEquals(0, count)
    }

    fun loadMovieData() {

        val countBefore: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        logger.info { "graph count before movies loaded = $countBefore" }

        val cypher = Neo4jServiceTestBase::class.java.getResource("/cypher/create-movies.cypher").readText()
        neo4jService.execute(cypher) {
            logger.info { "Loaded Movie Graph Data" }
        }

        val countAfter: Long = neo4jService.queryForObject("match (n) return count(n)")!!
        logger.info { "graph count after movies loaded = $countAfter" }
        Assertions.assertThat(countAfter).isGreaterThan(countBefore)

    }

    companion object {

        val JSON_100_TESTDATA = "generator/suppliers-10.json"

        val JSON_TESTDATA_MERGE = """
            MERGE (s:Supplier {supplierId : supplier.supplierId})
              SET s.name = supplier.name, s.discount = supplier.discount
            FOREACH (product in supplier.products |
                MERGE (p:Product {productId : product.productId})
                  SET p.name = product.name, p.category = product.category
                MERGE (p)-[:FROM]->(s)
            )
            RETURN count(s) as totalSuppliers
        """.trimIndent()

        val CSV_100_TESTDATA = "generator/products-100.csv"
        val CSV_1000_TESTDATA = "generator/products-1000.csv"
        val CSV_10000_TESTDATA = "generator/products-10000.csv"
        val CSV_50000_TESTDATA = "generator/products-50000.csv"

        fun findTestDataFile(filename:String) = File(".").walkTopDown().first { it.name == filename }.absolutePath

        // merge from csv read `row` variable data
        // (note: neo4j converts single word columns to uppercase, while columns with spaces need back-quotes to read)
        // columns from test data file...
        // ProductId,SupplierId,Name,Category,Units,Clearance,BestBefore

        val CSV_TESTDATA_MERGE_APOC = """
            MERGE (s:Supplier {supplierId: row.SUPPLIERID})
            MERGE (p:Product {product: row.PRODUCTID})
              SET p.name = row.NAME,
                  p.category = row.CATEGORY,
                  p.clearance = row.CLEARANCE
              MERGE (p)-[:FROM]->(s)
              RETURN count(p) as totalProducts
        """.trimIndent()

        val CSV_TESTDATA_MERGE_NEO4J = """
            MERGE (s:Supplier {supplierId: row.SupplierId})
            MERGE (p:Product {product: row.ProductId})
              SET p.name = row.Name,
                  p.category = row.Category,
                  p.clearance = row.Clearance
              MERGE (p)-[:FROM]->(s)
              RETURN count(p) as totalProducts
        """.trimIndent()

    }

}


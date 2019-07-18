package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import me.roybailey.neo4k.dsl.MarkdownProperties
import me.roybailey.neo4k.dsl.QueryStatement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


/**
 * These tests use queries from the Neo4j `play: northwind` tutorial
 */
abstract class Neo4jServiceNorthwindScriptTest(final override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {

    private val expectedNumberOfProducts = 77L
    private val expectedNumberOfCategories = 8L
    private val expectedNumberOfSuppliers = 29L
    private val expectedNumberOfCustomers = 91L
    private val expectedNumberOfOrders = 830L
    private val expectedCustomersWithOrders = 63L



    @Test
    fun `should load northwind dataset through asciidoc script`() {

        val queries = MarkdownProperties.loadFromClasspath("/queries/northwind.adoc", this)
        queries.entries.forEachIndexed { index, query -> logger.info { "$index) key=${query.key} value=${query.value}" } }
        assertThat(queries).hasSize(4)

        val scripts = QueryStatement.parseQueryScriptStatements(queries)
        loadProductCatalog(scripts.getOrElse("Product Catalog") { emptyList() })
        queryProductCatalog(scripts.getOrElse("Querying Product Catalog") { emptyList() })
        loadCustomerOrders(scripts.getOrElse("Customer Orders") { emptyList() })
        queryCustomerOrders(scripts.getOrElse("Query Customer Orders") { emptyList() })
    }


    private fun loadProductCatalog(scriptProductCatalog: List<QueryStatement>) {
        assertThat(scriptProductCatalog).hasSize(8)

        scriptProductCatalog.let { statements ->

            statements.forEach { statement ->
                logger.info { "query=\n\n$statement\n\n" }
                neo4jService.execute(statement.query)
            }
            neo4jService.execute("match (n:Product) return count(n) as totalProducts") {
                assertThat(it.single()["totalProducts"]).isEqualTo(expectedNumberOfProducts)
            }
            neo4jService.execute("match (n:Category) return count(n) as totalCategories") {
                assertThat(it.single()["totalCategories"]).isEqualTo(expectedNumberOfCategories)
            }
            neo4jService.execute("match (n:Supplier) return count(n) as totalSuppliers") {
                assertThat(it.single()["totalSuppliers"]).isEqualTo(expectedNumberOfSuppliers)
            }
            neo4jService.execute("match (p:Product)-[r]-(c:Category) return count(r) as productToCategory") {
                assertThat(it.single()["productToCategory"]).isEqualTo(expectedNumberOfProducts)
            }
            neo4jService.execute("match (p:Product)-[r]-(s:Supplier) return count(r) as productToSupplier") {
                assertThat(it.single()["productToSupplier"]).isEqualTo(expectedNumberOfProducts)
            }
        }
    }


    private fun queryProductCatalog(scriptQueryProductCatalog: List<QueryStatement>) {
        assertThat(scriptQueryProductCatalog).hasSize(2)

        scriptQueryProductCatalog.let { statements ->

            var numberOfSuppliersFound: Long = 0
            statements[0].let { supplierProductCategories ->
                logger.info { "query=\n\n$supplierProductCategories\n\n" }
                neo4jService.execute(supplierProductCategories.query) {
                    while (it.hasNext()) {
                        val record = it.next()
                        assertThat(record["Company"]).isNotNull()
                        assertThat(record["Categories"] as List<*>).isNotEmpty
                        ++numberOfSuppliersFound
                    }
                }
            }
            assertThat(numberOfSuppliersFound).isEqualTo(expectedNumberOfSuppliers)

            var numberOfProduceSuppliers: Long = 0
            statements[1].let { findProduceSuppliers ->
                logger.info { "query=\n\n$findProduceSuppliers\n\n" }
                neo4jService.execute(findProduceSuppliers.query) {
                    while (it.hasNext()) {
                        val record = it.next()
                        assertThat(record["ProduceSuppliers"]).isNotNull()
                        ++numberOfProduceSuppliers
                    }
                }
            }
            assertThat(numberOfProduceSuppliers).isEqualTo(5)
        }
    }


    private fun loadCustomerOrders(scriptCustomerOrders: List<QueryStatement>) {
        assertThat(scriptCustomerOrders).hasSize(6)

        scriptCustomerOrders.let { statements ->

            statements.forEach { statement ->
                logger.info { "query=\n\n$statement\n\n" }
                neo4jService.execute(statement.query)
            }
            neo4jService.execute("match (c:Customer) return count(c) as totalCustomers") {
                assertThat(it.single()["totalCustomers"]).isEqualTo(expectedNumberOfCustomers)
            }
            neo4jService.execute("match (o:Order) return count(o) as totalOrders") {
                assertThat(it.single()["totalOrders"]).isEqualTo(expectedNumberOfOrders)
            }
            neo4jService.execute("match (c:Customer)-[r]-(o:Order) return count(r) as totalOrdersToCustomers") {
                assertThat(it.single()["totalOrdersToCustomers"]).isEqualTo(expectedNumberOfOrders)
            }
        }
    }


    private fun queryCustomerOrders(scriptQueryCustomerOrders: List<QueryStatement>) {
        assertThat(scriptQueryCustomerOrders).hasSize(1)

        scriptQueryCustomerOrders.let { statements ->

            var numberOfCustomersFound: Long = 0
            statements[0].let { customerPurchases ->
                logger.info { "query=\n\n$customerPurchases\n\n" }
                neo4jService.execute(customerPurchases.query) {
                    while (it.hasNext()) {
                        val record = it.next()
                        assertThat(record["CustomerName"]).isNotNull()
                        assertThat(record["TotalProductsPurchased"] as Long).isGreaterThan(0L)
                        ++numberOfCustomersFound
                    }
                }
            }
            assertThat(numberOfCustomersFound).isEqualTo(expectedCustomersWithOrders)
        }
    }
}

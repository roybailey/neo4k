package me.roybailey.neo4k.api

import me.roybailey.neo4k.UnitTestBase
import me.roybailey.neo4k.api.Neo4jApoc.Companion.apocGetStaticAsJson
import me.roybailey.neo4k.api.Neo4jApoc.Companion.apocGetStaticAsString
import me.roybailey.neo4k.api.ScriptDsl.scriptLibrary
import me.roybailey.neo4k.api.ScriptDsl.toAsciiDoc
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.FileWriter


class QueryDslTest : UnitTestBase() {


    private fun logScriptStatements(library: ScriptLibrary) {

        logger.info { "\n\n" }
        library.entries.forEachIndexed { scriptIndex, entry ->
            logger.info { "${scriptIndex + 1}) ${entry.key}" }
            entry.value.forEachIndexed { statementIndex, queryStatement ->
                logger.info { "${scriptIndex + 1}.${statementIndex + 1}) ${queryStatement.description}" }
                queryStatement.defaultParams.forEach {
                    logger.info { "    $it" }
                }
                logger.info { "    defaultParams=${queryStatement.defaultParams}\n\n${queryStatement.query}\n\n\n" }
            }
        }

    }

    private fun saveScriptStatements(library: ScriptLibrary, title: String, customerFilename: String? = null) {

        val filename = customerFilename?.let { it } ?: title.replace(" ", "_") + ".adoc"
        toAsciiDoc(library, FileWriter("target/$filename"), title)
    }


    @Test
    fun `test query dsl simple cypher statement`(testInfo: TestInfo) {

        val scriptName = testInfo.testMethod.get().name
        val actual = scriptLibrary {
            script(scriptName) {}
            statement {
                description = "Creates a movie node"
                defaultParams = mapOf("title" to "DSL Movie Title")
                query = """create (m:Movie { title: __title }) return m""".toNeo4j()
            }
        }
        logScriptStatements(actual)

        SoftAssertions().apply {
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys).contains(scriptName)
            assertThat(actual[scriptName]).hasSize(1)
            assertThat(actual[scriptName]!![0].query).isEqualTo("""create (m:Movie { title: __title }) return m""".toNeo4j())
        }.assertAll()

        saveScriptStatements(actual, scriptName)
    }


    @Test
    fun `test query dsl complex cypher statement`(testInfo: TestInfo) {

        val scriptName = testInfo.testMethod.get().name
        val actual = scriptLibrary {
            script(scriptName) {}
            statement {
                description = "Store a url and auth token"
                defaultParams = mapOf(
                        "url" to "http://dev.replacewithrealdomain.com:8888/",
                        "auth" to "YOUR_AUTH_TOKEN"
                )
                query = """
                    CALL apoc.static.set('API_URL.MYSERVICE','{ url: "__url", auth: "Bearer __token" }')
                    """.toNeo4j().trimIndent()
            }
            statement {
                description = "Create Index on Products"
                query = """
                    CREATE INDEX ON :Product(productId)
                    """.toNeo4j().trimIndent()
            }
            statement {
                description = "Load all Products with complete append"
                query = """
                    CALL apoc.static.get('API_URL.MYSERVICE') yield value with apoc.convert.fromJsonMap(apoc.convert.toString(value)) as API
                    WITH COALESCE(API.url,'') as API_ROOT, COALESCE(API.auth,'') as API_AUTH
                    CALL apoc.load.jsonParams(API_ROOT+'/products',{Authorization: API_AUTH},null) YIELD value as row WITH row
                    MERGE (prd:Product { productId: row.productId })
                       ON CREATE SET prd += row
                    RETURN count(prd) as totalProducts
                    """.toNeo4j().trimIndent()
            }
        }

        logScriptStatements(actual)

        SoftAssertions().apply {
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys).contains(scriptName)
            assertThat(actual[scriptName]).hasSize(3)
            assertThat(actual[scriptName]!![0].query).isEqualTo("""CALL apoc.static.set('API_URL.MYSERVICE','{ url: "__url", auth: "Bearer __token" }')""".toNeo4j())
        }.assertAll()

        saveScriptStatements(actual, scriptName)
    }


    @Test
    fun `test query dsl apocLoadJson cypher statement`(testInfo: TestInfo) {

        val scriptName = testInfo.testMethod.get().name
        val actual = scriptLibrary {
            script(scriptName) {}
            statement {
                description = "Load all Products with cypher string"
                query = """
                    CALL apoc.static.get('API_URL.MYSERVICE') yield value WITH apoc.convert.fromJsonMap(apoc.convert.toString(value)) as API
                    WITH COALESCE(API.url,'') as API_ROOT, COALESCE(API.auth,'') as API_AUTH
                    CALL apoc.load.jsonParams(API_ROOT,{Authorization: API_AUTH},null,"/products") YIELD value WITH value
                    MERGE (prd:Product { productId: row.productId })
                       ON CREATE SET prd += row
                    RETURN count(prd) as totalProducts
                    """.toNeo4j().trimIndent()
            }
            statement {
                description = "Load all Products with cypher dsl"
                cypher {
                    append(apocGetStaticAsJson("API_URL.MYSERVICE", "API"))
                    append("WITH COALESCE(API.url,'') as API_ROOT, COALESCE(API.auth,'') as API_AUTH")
                    apocLoadJson {
                        url = "API_ROOT"
                        headers = mutableMapOf("Authorization" to "API_AUTH")
                        path = "/products".quoted()
                        cypher {
                            append("""
                                MERGE (prd:Product { productId: row.productId })
                                   ON CREATE SET prd += row
                                RETURN count(prd) as totalProducts
                                """)
                        }
                    }
                }
            }
        }

        logScriptStatements(actual)

        SoftAssertions().apply {
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys).contains(scriptName)
            assertThat(actual[scriptName]).hasSize(2)
            assertThat(actual[scriptName]!![1].query).isEqualTo(actual[scriptName]!![0].query)
        }.assertAll()

        saveScriptStatements(actual, scriptName)
    }


    @Test
    fun `test query dsl apocPeriodicLoadJson cypher statement`(testInfo: TestInfo) {

        val scriptName = testInfo.testMethod.get().name
        val actual = scriptLibrary {
            script(scriptName) {}
            statement {
                description = "Load all Products (periodic commit) with cypher string"
                query = """
                    CALL apoc.periodic.iterate("
                        CALL apoc.static.get('API_URL.MYSERVICE') yield value with apoc.convert.fromJsonMap(apoc.convert.toString(value)) as API
                        WITH COALESCE(API.url,'') as API_ROOT, COALESCE(API.auth,'') as API_AUTH
                        CALL apoc.load.jsonParams(API_ROOT+'/products',{Authorization: API_AUTH},null) YIELD value as row WITH row
                    ","
                        MERGE (prd:Product { productId: row.productId })
                          SET prd.status = row.status
                        MERGE (ptype:ProductType { productType: row.productType })
                        MERGE (prd)-[:OF]->(ptype)
                        WITH row, prd
                        MATCH (sup { supplierId: row.supplierId })
                        MERGE (prd)-[:BELONGS_TO]->(sup)
                        RETURN count(prd)
                    ", {batchSize:100, parallel:false})
                    """.toNeo4j().trimIndent()
            }
            statement {
                description = "Load all Products (periodic commit) with cypher dsl"
                cypher {
                    apocPeriodicIterate {
                        batchSize = 100
                        parallel = false
                        outer = """
                                CALL apoc.static.get('API_URL.MYSERVICE') yield value with apoc.convert.fromJsonMap(apoc.convert.toString(value)) as API
                                WITH COALESCE(API.url,'') as API_ROOT, COALESCE(API.auth,'') as API_AUTH
                                CALL apoc.load.jsonParams(API_ROOT+'/products',{Authorization: API_AUTH},null) YIELD value as row WITH row
                                """.trimIndent()
                        inner = """
                                MERGE (prd:Product { productId: row.productId })
                                  SET prd.status = row.status
                                MERGE (ptype:ProductType { productType: row.productType })
                                MERGE (prd)-[:OF]->(ptype)
                                WITH row, prd
                                MATCH (sup { supplierId: row.supplierId })
                                MERGE (prd)-[:BELONGS_TO]->(sup)
                                RETURN count(prd)
                                """.trimIndent()
                    }
                }
            }
        }

        logScriptStatements(actual)

        SoftAssertions().apply {
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys).contains(scriptName)
            assertThat(actual[scriptName]).hasSize(2)
            assertThat(actual[scriptName]!![1].query).isEqualTo(actual[scriptName]!![0].query)
        }.assertAll()

        saveScriptStatements(actual, scriptName)
    }

    @Test
    fun `test query dsl apocLoadJdbc cypher statement`(testInfo: TestInfo) {

        val scriptName = testInfo.testMethod.get().name
        val actual = scriptLibrary {
            script(scriptName) {}
            statement {
                description = "Load all Products with cypher string"
                query = """
                    CALL apoc.static.get('DBURL') yield value WITH apoc.convert.toString(value) AS URL
                    CALL apoc.load.jdbc(URL,
                        "SELECT * FROM PRODUCTS"
                    ) YIELD row WITH row
                    MERGE (prd:Product { productId: row.productId })
                       ON CREATE SET prd += row
                    RETURN count(prd) as totalProducts
                    """.toNeo4j().trimIndent()
            }
            statement {
                description = "Load all Products with cypher dsl"
                cypher {
                    append(apocGetStaticAsString("DBURL", "URL"))
                    apocLoadJdbc {
                        url = "URL"
                        select = "SELECT * FROM PRODUCTS"
                        cypher = """
                                MERGE (prd:Product { productId: row.productId })
                                   ON CREATE SET prd += row
                                RETURN count(prd) as totalProducts
                                """
                    }
                }
            }
        }

        logScriptStatements(actual)

        SoftAssertions().apply {
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys).contains(scriptName)
            assertThat(actual[scriptName]).hasSize(2)
            assertThat(actual[scriptName]!![1].query).isEqualTo(actual[scriptName]!![0].query)
        }.assertAll()

        saveScriptStatements(actual, scriptName)
    }


    @Test
    fun `test query dsl apocPeriodicLoadJdbc cypher statement`(testInfo: TestInfo) {

        val scriptName = testInfo.testMethod.get().name
        val actual = scriptLibrary {
            script(scriptName) {}
            statement {
                description = "Load all Products (periodic commit) with cypher string"
                query = """
                    CALL apoc.periodic.iterate("
                        CALL apoc.static.get('DBURL') yield value WITH apoc.convert.toString(value) AS URL
                        CALL apoc.load.jdbc(URL,"SELECT * FROM PRODUCTS") YIELD row WITH row
                    ","
                        MERGE (prd:Product { productId: row.productId })
                          SET prd.status = row.status
                        MERGE (ptype:ProductType { productType: row.productType })
                        MERGE (prd)-[:OF]->(ptype)
                        WITH row, prd
                        MATCH (sup { supplierId: row.supplierId })
                        MERGE (prd)-[:BELONGS_TO]->(sup)
                        RETURN count(prd)
                    ", {batchSize:100, parallel:false})
                    """.toNeo4j().trimIndent()
            }
            statement {
                description = "Load all Products (periodic commit) with cypher dsl"
                cypher {
                    apocPeriodicIterate {
                        batchSize = 100
                        parallel = false
                        outer = """
                                CALL apoc.static.get('DBURL') yield value WITH apoc.convert.toString(value) AS URL
                                CALL apoc.load.jdbc(URL,"SELECT * FROM PRODUCTS") YIELD row WITH row
                                """.trimIndent()
                        inner = """
                                MERGE (prd:Product { productId: row.productId })
                                  SET prd.status = row.status
                                MERGE (ptype:ProductType { productType: row.productType })
                                MERGE (prd)-[:OF]->(ptype)
                                WITH row, prd
                                MATCH (sup { supplierId: row.supplierId })
                                MERGE (prd)-[:BELONGS_TO]->(sup)
                                RETURN count(prd)
                                """.trimIndent()
                    }
                }
            }
            statement {
                description = "Load all Products (periodic commit) with cypher dsl"
                cypher {
                    apocPeriodicIterate {
                        batchSize = 100
                        parallel = false
                        outer {
                            append("CALL apoc.static.get('DBURL') yield value WITH apoc.convert.toString(value) AS URL")
                            append("CALL apoc.load.jdbc(URL,\"SELECT * FROM PRODUCTS\") YIELD row WITH row")
                        }
                        inner = """
                                MERGE (prd:Product { productId: row.productId })
                                  SET prd.status = row.status
                                MERGE (ptype:ProductType { productType: row.productType })
                                MERGE (prd)-[:OF]->(ptype)
                                WITH row, prd
                                MATCH (sup { supplierId: row.supplierId })
                                MERGE (prd)-[:BELONGS_TO]->(sup)
                                RETURN count(prd)
                                """.trimIndent()
                    }
                }
            }
        }

        logScriptStatements(actual)

        SoftAssertions().apply {
            assertThat(actual.size).isEqualTo(1)
            assertThat(actual.keys).contains(scriptName)
            assertThat(actual[scriptName]).hasSize(3)
            assertThat(actual[scriptName]!![1].query).isEqualTo(actual[scriptName]!![0].query)
            assertThat(actual[scriptName]!![2].query).isEqualTo(actual[scriptName]!![0].query)
        }.assertAll()

        saveScriptStatements(actual, scriptName)
    }
}


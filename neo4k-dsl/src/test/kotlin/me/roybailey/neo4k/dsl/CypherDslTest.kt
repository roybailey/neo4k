package me.roybailey.neo4k.dsl

import me.roybailey.neo4k.dsl.CypherDsl.cypher
import me.roybailey.neo4k.testdata.UnitTestBase
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.*
import java.io.FileWriter


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CypherDslTest : UnitTestBase() {


    companion object {

        var asciiDoc: FileWriter = FileWriter(projectFolder + "/CypherDsl.adoc")

        @JvmStatic
        @BeforeAll
        private fun openDocumentFile() {
            logger.info { "Opened Document File(s) ...." }
            CypherDocumentation.toAsciiDocTableOfContents(asciiDoc, "CypherDSL")
        }

        private fun saveCypherDslDocumentation(title: String, code: String, cypher: String): String {
            logger.info { "$title\n\n$code\n\n$cypher\n\n" }
            CypherDocumentation.toAsciiDoc(asciiDoc, title, code, cypher)
            return cypher
        }

        @JvmStatic
        @AfterAll
        private fun closeDocumentFile() {
            logger.info { "Closed Document File(s) ...." }
            asciiDoc.flush()
            asciiDoc.close()
        }
    }


    private fun title(testInfo: TestInfo) = testInfo.testMethod.get().name.replace("test", "").trim()


    @Test
    fun `test Cypher Fragments`() {

        SoftAssertions().apply {
            assertThat(CypherDsl.quoted("test")).isEqualTo("\"test\"")
            assertThat(CypherDsl.quoted("test", false)).isEqualTo("'test'")
            assertThat(CypherDsl.quoted("test", false, false)).isEqualTo("test")

            assertThat(CypherDsl.withLabels("test")).isEqualTo("test")
            assertThat(CypherDsl.withLabels("test", "one")).isEqualTo("test:one")
            assertThat(CypherDsl.withLabels("test", "one", "two")).isEqualTo("test:one:two")
        }.assertAll()
    }


    @Test
    @Order(1)
    fun `test Simple Create Cypher DSL`(testInfo: TestInfo) {

        val actual = saveCypherDslDocumentation(
                title = title(testInfo),
                code = """
                cypher {
                    CREATE("m:Movie { title: __title }")
                    RETURN("m")
                }
                """.trimIndent(),
                cypher =
                // TODO copy the cypher dsl block here into the string above to capture for documentation
                cypher {
                    CREATE("m:Movie { title: __title }")
                    RETURN("m")
                }
        )

        SoftAssertions().apply {
            assertThat(actual.trim()).isEqualTo("""
                CREATE (m:Movie { title: __title })
                RETURN m
                """.trimIndent().toNeo4j())
        }.assertAll()
    }


    @Test
    @Order(2)
    fun `test Create with Paramters Cypher DSL`(testInfo: TestInfo) {

        val actual = saveCypherDslDocumentation(
                title = title(testInfo),
                code = """
                cypher {
                    CREATE("TheMatrix", "Movie") {
                        param(
                            "title" to "The Matrix".singleQuoted(),
                            "released" to "1999",
                            "tagline" to "Welcome to the Real World".singleQuoted()
                        )
                    }
                }
                """.trimIndent(),
                cypher =
                // TODO copy the cypher dsl block here into the string above to capture for documentation
                cypher {
                    CREATE("TheMatrix", "Movie") {
                        param(
                            "title" to "The Matrix".singleQuoted(),
                            "released" to "1999",
                            "tagline" to "Welcome to the Real World".singleQuoted()
                        )
                    }
                }
        )

        SoftAssertions().apply {
            assertThat(actual.trim()).isEqualTo("""
                CREATE (TheMatrix:Movie { title:'The Matrix', released:1999, tagline:'Welcome to the Real World' })
                """.trimIndent().toNeo4j())
        }.assertAll()
    }


    @Test
    @Order(3)
    fun `test Create and Relationship with Parameters Cypher DSL`(testInfo: TestInfo) {

        val title = title(testInfo)
        val TheMarix = "TheMatrix"
        val Keanu = "Keanu"
        val Carrie = "Carrie"
        val actual =
                cypher {
                    CREATE(TheMarix, "Movie") {
                        param("title" to "The Matrix".singleQuoted(),
                                "released" to 1999,
                                "tagline" to "Welcome to the Real World".singleQuoted())
                    }
                    CREATE(Keanu, "Person") {
                        param("name" to "Keanu Reeves".singleQuoted(),
                                "born" to 1964)
                    }
                    CREATE(Carrie, "Person") {
                        param("name" to "Carrie-Anne Moss".singleQuoted(), "born" to 1967)
                    }
                    CREATE()
                    +relationship(Keanu, "ACTED_IN", TheMarix) {
                        param("roles", arrayOf("Neo".singleQuoted()))
                    }
                    +",\n"
                    +relationship(Carrie, "ACTED_IN", TheMarix) {
                        param("roles", arrayOf("Trinity".singleQuoted()))
                    }
                }
        val code = """
                cypher {
                    CREATE(TheMarix, "Movie") {
                        param("title" to "The Matrix".singleQuoted(),
                                "released" to 1999,
                                "tagline" to "Welcome to the Real World".singleQuoted())
                    }
                    CREATE(Keanu, "Person") {
                        param("name" to "Keanu Reeves".singleQuoted(),
                                "born" to 1964)
                    }
                    CREATE(Carrie, "Person") {
                        param("name" to "Carrie-Anne Moss".singleQuoted(), "born" to 1967)
                    }
                    CREATE()
                    +relationship(Keanu, "ACTED_IN", TheMarix) {
                        param("roles", arrayOf("Neo".singleQuoted()))
                    }
                    +",\n"
                    +relationship(Carrie, "ACTED_IN", TheMarix) {
                        param("roles", arrayOf("Trinity".singleQuoted()))
                    }
                }
        """.trimIndent()
        saveCypherDslDocumentation(title, code, actual)

        SoftAssertions().apply {
            assertThat(actual.trimMargin()).isEqualTo("""
                CREATE (TheMatrix:Movie { title:'The Matrix', released:1999, tagline:'Welcome to the Real World' })
                CREATE (Keanu:Person { name:'Keanu Reeves', born:1964 })
                CREATE (Carrie:Person { name:'Carrie-Anne Moss', born:1967 })
                CREATE
                (Keanu)-[:ACTED_IN { roles:['Neo'] }]->(TheMatrix),
                (Carrie)-[:ACTED_IN { roles:['Trinity'] }]->(TheMatrix)
                """.trimIndent().toNeo4j())
        }.assertAll()
    }

}


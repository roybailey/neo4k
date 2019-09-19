package me.roybailey.neo4k.dsl

import me.roybailey.neo4k.testdata.UnitTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class QueryApiTest : UnitTestBase() {


    @Test
    fun `test parameter parsing regexp`() {
        """
            match (m:Movie { title : __title })

        """.toNeo4j().trimIndent().let {
            val paramSearch = Regex(QueryStatement.REGEX_PARAM)
            val all = paramSearch.findAll(it)
            val matches = all.map {  it.value }.toList()
            assertThat(matches).hasSize(1)
            assertThat(matches[0]).isEqualTo("${'$'}title")
        }

        """
            match (m:Movie)
            where m.title = __title and m.released > __releasedSince

        """.toNeo4j().trimIndent().let {
            val paramSearch = Regex(QueryStatement.REGEX_PARAM)
            val all = paramSearch.findAll(it)
            val matches = all.map {  it.value }.toList()
            assertThat(matches).hasSize(2)
            assertThat(matches[0]).isEqualTo("${'$'}title")
            assertThat(matches[1]).isEqualTo("${'$'}releasedSince")
        }
    }


    @Test
    fun `test statement parsing`() {

        val statements = QueryStatement.parseQueryScriptStatements("""

            // query movie by title
            match (m:Movie { title: ${'$'}title }

            // query movie by title since released date
            match (m:Movie { title: ${'$'}title }
            where m.released > ${'$'}released

            // Create indexes
            CREATE INDEX ON :Product(productID);
            CREATE INDEX ON :Category(categoryID);

        """.trimIndent())
        assertThat(statements).hasSize(4)
        statements[0].let {
            assertThat(it.description).isEqualToIgnoringCase("// query movie by title")
            assertThat(it.query).isEqualToIgnoringCase("match (m:Movie { title: ${'$'}title }")
            assertThat(it.defaultParams).hasSize(1)
            assertThat(it.defaultParams).containsKey("title")
        }
        statements[1].let {
            assertThat(it.description).isEqualToIgnoringCase("// query movie by title since released date")
            assertThat(it.query).isEqualToIgnoringCase("match (m:Movie { title: ${'$'}title }\nwhere m.released > ${'$'}released")
            assertThat(it.defaultParams).hasSize(2)
            assertThat(it.defaultParams).containsKeys("title","released")
        }
        statements[2].let {
            assertThat(it.description).isEqualToIgnoringCase("// Create indexes")
            assertThat(it.query).isEqualToIgnoringCase("CREATE INDEX ON :Product(productID)")
            assertThat(it.defaultParams).hasSize(0)
        }
        statements[3].let {
            assertThat(it.description).isEqualToIgnoringCase("// Create indexes...2")
            assertThat(it.query).isEqualToIgnoringCase("CREATE INDEX ON :Category(categoryID)")
            assertThat(it.defaultParams).hasSize(0)
        }
    }


    @Test
    fun `test multiple cypher statements from script file`() {

        val script = QueryApiTest::class.java.getResource("/cypher/time-tree-create.cypher").readText()
        val statements = QueryStatement.parseQueryScriptStatements(script)
        statements.forEachIndexed { index, statement -> logger.info { "$index) query=$statement" } }
        assertThat(statements).hasSize(4)
    }


    @Test
    fun `test multiple named cypher statements from adoc`() {

        val queries = MarkdownProperties.loadFromClasspath("/queries/time-tree.adoc", this)
        queries.entries.forEachIndexed { index, query -> logger.info { "$index) key=${query.key} value=${query.value}" } }
        assertThat(queries).hasSize(2)

        val statements = QueryStatement.parseQueryScriptStatements(queries)
        statements.entries.forEachIndexed { index, entry -> logger.info { "$index) key=${entry.key} query=${entry.value}" } }
        assertThat(statements["createTimeTree"]).hasSize(4)
        assertThat(statements["deleteTimeTree"]).hasSize(1)
    }

}

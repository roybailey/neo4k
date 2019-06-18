package me.roybailey.neo4k.api

import me.roybailey.neo4k.util.MarkdownProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class QueryApiTest : BaseTest() {


    @Test
    fun `test parameter parsing regexp`() {
        Neo4jCypher.toNeo4j("""
            match (m:Movie { title : __title })
        """.trimIndent()).let {
            val paramSearch = Regex(QueryStatement.REGEX_PARAM)
            val all = paramSearch.findAll(it)
            val matches = all.map {  it.value }.toList()
            assertThat(matches).hasSize(1)
            assertThat(matches[0]).isEqualTo("${'$'}title")
        }

        Neo4jCypher.toNeo4j("""
            match (m:Movie)
            where m.title = __title and m.released > __releasedSince
        """.trimIndent()).let {
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
        """.trimIndent())
        assertThat(statements).hasSize(1)
        assertThat(statements[0].description).isEqualToIgnoringCase("// query movie by title")
        assertThat(statements[0].query).isEqualToIgnoringCase("match (m:Movie { title: ${'$'}title }")
        assertThat(statements[0].defaultParams).hasSize(1)
        assertThat(statements[0].defaultParams).containsKey("title")
    }


    @Test
    fun `test multiple cypher statements from script file`() {

        val script = QueryApiTest::class.java.getResource("/cypher/time-tree-create.cypher").readText()
        val statements = QueryStatement.parseQueryScriptStatements(script)
        statements.forEachIndexed { index, statement -> LOG.info { "$index) query=$statement" } }
        assertThat(statements).hasSize(4)
    }


    @Test
    fun `test multiple named cypher statements from adoc`() {

        val queries = MarkdownProperties.loadFromClasspath("/queries/time-tree.adoc", this)
        queries.entries.forEachIndexed { index, query -> LOG.info { "$index) key=${query.key} value=${query.value}" } }
        assertThat(queries).hasSize(2)

        val statements = QueryStatement.parseQueryScriptStatements(queries)
        statements.entries.forEachIndexed { index, entry -> LOG.info { "$index) key=${entry.key} query=${entry.value}" } }
        assertThat(statements["createTimeTree"]).hasSize(4)
        assertThat(statements["deleteTimeTree"]).hasSize(1)
    }

}

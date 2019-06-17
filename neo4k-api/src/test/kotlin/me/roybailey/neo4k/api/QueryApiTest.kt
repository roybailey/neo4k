package me.roybailey.neo4k.api

import me.roybailey.neo4k.util.MarkdownProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class QueryApiTest : BaseTest() {


    @Test
    fun `test multiple cypher statements from script file`() {

        val script = QueryApiTest::class.java.getResource("/cypher/time-tree-create.cypher").readText()
        val statements = QueryStatement.extractQueryScriptStatements(script)
        statements.forEachIndexed { index, statement -> LOG.info { "$index) statement=$statement" } }
        assertThat(statements).hasSize(4)
    }


    @Test
    fun `test multiple named cypher statements from adoc`() {

        val queries = MarkdownProperties.loadFromClasspath("/queries/time-tree.adoc", this)
        queries.entries.forEachIndexed { index, query -> LOG.info { "$index) key=${query.key} value=${query.value}" } }
        assertThat(queries).hasSize(2)

        val statements = QueryStatement.extractQueryScriptStatements(queries)
        statements.entries.forEachIndexed { index, entry -> LOG.info { "$index) key=${entry.key} statement=${entry.value}" } }
        assertThat(statements["createTimeTree"]).hasSize(4)
        assertThat(statements["deleteTimeTree"]).hasSize(1)
    }

}

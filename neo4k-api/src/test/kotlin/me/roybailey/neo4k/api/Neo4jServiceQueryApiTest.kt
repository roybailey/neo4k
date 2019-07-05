package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import me.roybailey.neo4k.util.MarkdownProperties
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


abstract class Neo4jServiceQueryApiTest(final override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {

    @Test
    fun `should load time tree from script files`() {

        val script = QueryApiTest::class.java.getResource("/cypher/time-tree-create.cypher").readText()
        val statements = QueryStatement.parseQueryScriptStatements(script)
        Assertions.assertThat(statements).hasSize(4)
        statements.forEachIndexed { index, statement ->
            logger.info { "------------------------------------------------------------" }
            logger.info { "$index) query=$statement (params=${statement.defaultParams.keys})" }
            val parameters = mutableMapOf<String, Any>()
            statement.defaultParams.keys.forEach { key ->
                when (key) {
                    "from" -> parameters["from"] = 2016
                    "upto" -> parameters["upto"] = 2020
                }
            }
            neo4jService.execute(statement.query, parameters)
        }

        val years = neo4jService.queryForObject<Long>("match (y:Year) return count(y) as years")
        assertThat(years).isEqualTo(5L)
        val months = neo4jService.queryForObject<Long>("match (m:Month) return count(m) as months")
        assertThat(months).isEqualTo(60L)
        val days = neo4jService.queryForObject<Long>("match (d:Day) return count(d) as days")
        assertThat(days).isEqualTo(1825L)
    }

    @Test
    fun `should load time tree from asciidoc file`() {

        val queries = MarkdownProperties.loadFromClasspath("/queries/time-tree.adoc", this)
        queries.entries.forEachIndexed { index, query -> logger.info { "$index) key=${query.key} value=${query.value}" } }
        assertThat(queries).hasSize(2)

        val statements = QueryStatement.parseQueryScriptStatements(queries)
        statements.entries.forEachIndexed { index, entry -> logger.info { "$index) key=${entry.key} query=${entry.value}" } }
        assertThat(statements["createTimeTree"]).hasSize(4)
        assertThat(statements["deleteTimeTree"]).hasSize(1)

        statements["createTimeTree"]?.forEachIndexed { index, statement ->
            logger.info { "------------------------------------------------------------" }
            logger.info { "$index) query=$statement" }
            val parameters = mutableMapOf<String, Any>()
            statement.defaultParams.keys.forEach { key ->
                when (key) {
                    "from" -> parameters["from"] = 2016
                    "upto" -> parameters["upto"] = 2020
                }
            }
            neo4jService.execute(statement.query, parameters)
        }

        val years = neo4jService.queryForObject<Long>("match (y:Year) return count(y) as years")
        assertThat(years).isEqualTo(5L)
        val months = neo4jService.queryForObject<Long>("match (m:Month) return count(m) as months")
        assertThat(months).isEqualTo(60L)
        val days = neo4jService.queryForObject<Long>("match (d:Day) return count(d) as days")
        assertThat(days).isEqualTo(1825L)
    }

}

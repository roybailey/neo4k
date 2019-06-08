package me.roybailey.neo4k.api

import mu.KotlinLogging
import org.assertj.core.api.Assertions


open class BaseNeo4jRecordTest {

    val LOG = KotlinLogging.logger { this::class.qualifiedName }

    protected fun getNeo4jRecord(): Neo4jServiceRecord {

        val fields: Map<String, Any> = mapOf(
                "string" to "hello",
                "boolean" to true,
                "node" to emptyMap<String, Any>())

        return object : Neo4jServiceRecord {
            override fun keys(): List<String> = fields.keys.map { it }
            override fun values(): List<Any> = fields.values.map { it }
            override fun containsKey(lookup: String): Boolean = fields.containsKey(lookup)
            override fun index(lookup: String): Int = keys().indexOf(lookup)
            override fun get(key: String): Any = fields.getOrDefault(key, Unit)
            override fun get(index: Int): Any = fields()[index].second
            override fun size(): Int = fields.size
            override fun asMap(): Map<String, Any> = fields
            override fun fields(): List<Pair<String, Any>> = fields.entries.map { Pair(it.key, it.value) }
        }
    }

    protected fun assertNeo4jRecord(actual:Neo4jServiceRecord, expected:Neo4jServiceRecord) {

        Assertions.assertThat(actual.asMap()).isEqualTo(expected.asMap())
        Assertions.assertThat(actual.keys()).isEqualTo(expected.keys())
        val actualValues = actual.values()
        val expectedValues = expected.values()
        actualValues.forEachIndexed { index, any ->
            LOG.info { "$index actual=[${actualValues[index]::class.qualifiedName}] expected=[${expectedValues[index]::class.qualifiedName}]" }
        }
        Assertions.assertThat(actualValues.size).isEqualTo(expectedValues.size)
        Assertions.assertThat(actualValues).isEqualTo(expectedValues)
        Assertions.assertThat(actual.containsKey("string")).isEqualTo(expected.containsKey("string"))
        Assertions.assertThat(actual.get("string")).isEqualTo(expected.get("string"))
        Assertions.assertThat(actual.size()).isEqualTo(expected.size())
        Assertions.assertThat(actual.fields()).isEqualTo(expected.fields())
        Assertions.assertThat(actual.index("boolean")).isEqualTo(expected.index("boolean"))
        Assertions.assertThat(actual[0]).isEqualTo(expected[0])
        Assertions.assertThat(actual[1]).isEqualTo(expected[1])
        Assertions.assertThat(actual[2]).isEqualTo(expected[2])
    }
}

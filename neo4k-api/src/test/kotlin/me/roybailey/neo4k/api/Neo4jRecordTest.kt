package me.roybailey.neo4k.api

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat


open class Neo4jRecordTest {

    val LOG = KotlinLogging.logger { this::class.qualifiedName }


    protected fun createNeo4jServiceRecord(template: Map<String, Any>, vararg more: Pair<String, Any>): Neo4jServiceRecord {
        val fields = mutableMapOf<String, Any>()
                .also { it.putAll(template) }
                .also { it.putAll(more) }
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
            override fun equals(other: Any?): Boolean {
                if (other is Neo4jServiceRecord) {
                    return other.asMap() == asMap()
                }
                return false
            }
        }
    }


    protected fun createNeo4jDataRecord(vararg more: Pair<String, Any>): Neo4jServiceRecord =
            createNeo4jServiceRecord(mapOf(
                    "string" to "hello",
                    "boolean" to true,
                    "long" to 100L),
                    *more)


    protected fun createNeo4jGraphRecord(): Neo4jServiceRecord = createNeo4jServiceRecord(mapOf(
            "a" to createNeo4jDataRecord("id" to 1L),
            "b" to createNeo4jDataRecord("id" to 2L)))


    protected fun assertNeo4jRecord(actual: Neo4jServiceRecord, expected: Neo4jServiceRecord) {

        assertThat(actual.asMap()).isEqualTo(expected.asMap())
        assertThat(actual.keys()).isEqualTo(expected.keys())
        val actualValues = actual.values()
        val expectedValues = expected.values()
        assertThat(actualValues.size).isEqualTo(expectedValues.size)
        expectedValues.forEachIndexed { index, any ->
            LOG.info { "$index actual=[${actualValues[index]::class.qualifiedName}] expected=[${expectedValues[index]::class.qualifiedName}]" }
            when (expectedValues[index]) {
                is Neo4jServiceRecord -> {
                    assertThat((actualValues[index] as Neo4jServiceRecord).asMap())
                            .isEqualTo((expectedValues[index] as Neo4jServiceRecord).asMap())
                }
                else -> assertThat(actualValues[index]).isEqualTo(expectedValues[index])
            }
        }
        val actualKeys = actual.keys().toMutableList()
        expected.keys().forEachIndexed { indexed, key ->
            assertNeo4jServiceRecordValue(key, actual[key], expected[key])
            assertNeo4jServiceRecordValue(indexed.toString(), actual[indexed], expected[indexed])
            assertThat(actual.index(expected.keys()[indexed])).isEqualTo(expected.index(expected.keys()[indexed]))
            actualKeys.remove(key)
        }
        if (actualKeys.size > 0)
            LOG.warn { "Actual record has more keys than expected $actualKeys" }
        assertThat(actualKeys).isEmpty()
        assertThat(actual.size()).isEqualTo(expected.size())
        val actualFields = actual.fields()
        val expectedFields = expected.fields()
        expectedFields.forEachIndexed { index, pair ->
            assertThat(actualFields[index].first).isEqualTo(expectedFields[index].first)
            assertNeo4jServiceRecordValue(expectedFields[index].first, actualFields[index].second, expectedFields[index].second)
        }
    }

    private fun assertNeo4jServiceRecordValue(key:String, actualValue: Any?, expectedValue: Any?) {
        LOG.warn { "Asserting $key actual=$actualValue expected=$expectedValue" }
        LOG.warn { "Asserting $key actual=${actualValue!!::class.supertypes} expected=${expectedValue!!::class.supertypes}" }
        when (expectedValue) {
            is Neo4jServiceRecord -> assertThat((actualValue as Neo4jServiceRecord).asMap()).isEqualTo(expectedValue.asMap())
            else -> assertThat(actualValue).isEqualTo(expectedValue)
        }
    }
}

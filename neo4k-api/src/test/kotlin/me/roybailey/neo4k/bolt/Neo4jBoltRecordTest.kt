package me.roybailey.neo4k.bolt

import com.nhaarman.mockitokotlin2.*
import me.roybailey.neo4k.api.BaseNeo4jRecordTest
import me.roybailey.neo4k.api.Neo4jServiceRecord
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.neo4j.driver.internal.value.NodeValue
import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.Value
import org.neo4j.driver.v1.Values
import org.neo4j.driver.v1.types.Node
import org.neo4j.driver.v1.util.Function
import java.lang.Integer.parseInt


class Neo4jBoltRecordTest : BaseNeo4jRecordTest() {

    private fun boltPair(key: String, value: Any) =
            object : org.neo4j.driver.v1.util.Pair<String, Value> {
                override fun key(): String = key
                override fun value(): Value = boltValue(value)
            }

    private fun boltValue(data: Any): Value =
            when (data) {
                is Neo4jServiceRecord -> NodeValue(object : Node {
                    override fun values(): MutableIterable<Value> = data.values().map { boltValue(it) }.toMutableList()
                    override fun <T : Any?> values(p0: Function<Value, T>?): MutableIterable<T> = TODO("not implemented")
                    override fun id(): Long = if (data.containsKey("id")) data.get("id") as Long else 0L
                    override fun hasLabel(p0: String?): Boolean = TODO("not implemented")
                    override fun size(): Int = data.size()
                    override fun asMap(): MutableMap<String, Any> = mutableMapOf<String, Any>().also { it.putAll(data.asMap()) }
                    override fun <T : Any?> asMap(p0: Function<Value, T>?): MutableMap<String, T> = TODO("not implemented")
                    override fun containsKey(key: String?): Boolean = data.containsKey(key!!)
                    override fun get(key: String?): Value = boltValue(data.get(key!!))
                    override fun labels(): MutableIterable<String> = TODO("not implemented")
                    override fun keys(): MutableIterable<String> = data.keys().toMutableList()
                })
                else -> Values.value(data)
            }

    private fun mockBoltRecord(testData: Neo4jServiceRecord): Record {
        val record = mock<Record> {
            on { asMap() }.doReturn(testData.asMap())
            on { keys() }.doReturn(testData.keys())
            on { size() }.doReturn(testData.size())
            on { values() }.doReturn(testData.values().map { boltValue(it) })
            on { fields() }.doReturn(testData.fields().map { boltPair(it.first, it.second) })
        }
        doAnswer { onMock -> testData.containsKey(onMock.arguments[0].toString()) }
                .whenever(record).containsKey(anyString())
        doAnswer { onMock -> testData.index(onMock.arguments[0].toString()) }
                .whenever(record).index(anyString())
        doAnswer { onMock ->
            testData.get(onMock.arguments[0].toString()).run {
                when (this) {
                    is Neo4jServiceRecord -> boltValue(this)
                    else -> Values.value(this)
                }
            }
        }.whenever(record).get(anyString())
        doAnswer { onMock ->
            testData.get(parseInt(onMock.arguments[0].toString())).run {
                when (this) {
                    is Neo4jServiceRecord -> boltValue(this)
                    else -> Values.value(this)
                }
            }
        }.whenever(record).get(anyInt())
        return record
    }

    @Test
    fun `should map bolt table result record into Neo4jServiceRecord correctly`() {

        val testData = createNeo4jDataRecord()
        val record = mockBoltRecord(testData)
        assertNeo4jRecord(Neo4jBoltRecord(record), testData)
        verify(record, atLeast(2)).keys()
    }

    @Test
    fun `should map bolt graph result record into Neo4jServiceRecord correctly`() {

        val testData = createNeo4jGraphRecord()
        val record = mockBoltRecord(testData)
        assertNeo4jRecord(Neo4jBoltRecord(record), testData)
        verify(record, atLeast(2)).keys()
    }

}

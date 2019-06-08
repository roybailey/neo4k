package me.roybailey.neo4k.bolt

import com.nhaarman.mockitokotlin2.*
import me.roybailey.neo4k.api.BaseNeo4jRecordTest
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.Value
import org.neo4j.driver.v1.Values
import java.lang.Integer.parseInt


class Neo4jBoltRecordTest : BaseNeo4jRecordTest() {

    private fun neo4jPair(key: String, value: Any) =
            object : org.neo4j.driver.v1.util.Pair<String, Value> {
                override fun key(): String = key
                override fun value(): Value = Values.value(value)
            }


    @Test
    fun testNeo4jBoltRecord() {

        val testData = getNeo4jRecord()
        val record = mock<Record> {
            on { asMap() }.doReturn(testData.asMap())
            on { keys() }.doReturn(testData.keys())
            on { size() }.doReturn(testData.size())
            on { values() }.doReturn(testData.values().map { Values.value(it) })
            on { fields() }.doReturn(testData.fields().map { neo4jPair(it.first, it.second) })
        }
        doAnswer { onMock -> testData.containsKey(onMock.arguments[0].toString()) }
                .whenever(record).containsKey(anyString())
        doAnswer { onMock -> testData.index(onMock.arguments[0].toString()) }
                .whenever(record).index(anyString())
        doAnswer { onMock -> Values.value(testData.get(onMock.arguments[0].toString())) }
                .whenever(record).get(anyString())
        doAnswer { onMock -> Values.value(testData.get(parseInt(onMock.arguments[0].toString()))) }
                .whenever(record).get(anyInt())
        assertNeo4jRecord(Neo4jBoltRecord(record), testData)
        verify(record).keys()
    }

}

package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.BaseNeo4jRecordTest
import org.junit.jupiter.api.Test


class Neo4jBoltRecordTest : BaseNeo4jRecordTest() {

    @Test
    fun `should map bolt table result record into Neo4jServiceRecord correctly`() {

        val testData = createNeo4jDataRecord()
        val record = Neo4jBoltRecord.createBoltRecord(testData)
        assertNeo4jRecord(Neo4jBoltRecord(record), testData)
    }

    @Test
    fun `should map bolt graph result record into Neo4jServiceRecord correctly`() {

        val testData = createNeo4jGraphRecord()
        val record = Neo4jBoltRecord.createBoltRecord(testData)
        assertNeo4jRecord(Neo4jBoltRecord(record), testData)
    }

}

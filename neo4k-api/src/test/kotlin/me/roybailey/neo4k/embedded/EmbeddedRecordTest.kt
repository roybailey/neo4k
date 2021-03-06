package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.Neo4jRecordTest
import org.junit.jupiter.api.Test


class EmbeddedRecordTest: Neo4jRecordTest() {

    @Test
    fun `should map bolt table result record into Neo4jServiceRecord correctly`() {

        val testData = createNeo4jDataRecord()
        val record = Neo4jEmbeddedRecord.createEmbeddedRecord(testData)
        assertNeo4jRecord(Neo4jEmbeddedRecord(record), testData)
    }

    @Test
    fun `should map bolt graph result record into Neo4jServiceRecord correctly`() {

        val testData = createNeo4jGraphRecord()
        val record = Neo4jEmbeddedRecord.createEmbeddedRecord(testData)
        assertNeo4jRecord(Neo4jEmbeddedRecord(record), testData)
    }

}

package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.BaseNeo4jRecordTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo


class Neo4jEmbeddedRecordTest: BaseNeo4jRecordTest() {

    @Test
    fun testNeo4jEmbeddedRecord() {

        val testData = createNeo4jDataRecord()
        assertNeo4jRecord(Neo4jEmbeddedRecord(testData.asMap()), testData)
    }

}

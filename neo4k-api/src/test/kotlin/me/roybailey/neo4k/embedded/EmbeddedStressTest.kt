package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.Neo4jServiceStressTest
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_50000_TESTDATA
import me.roybailey.neo4k.api.Neo4jTestQueries.Companion.CSV_TESTDATA_MERGE_APOC
import java.io.File


/**
 * Test application to load really large datasets into Neo4j to check performance.
 * Not built as a test since it would materially impact the build cycle.
 */
fun main(args: Array<String>) {

    Neo4jServiceStressTest(EmbeddedNeo4jServiceFactory.createNeo4jService()).run {
        neo4jLoadCsvBatch10000()
        shutdown()
    }

    System.exit(0)
}


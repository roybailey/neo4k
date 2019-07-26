package me.roybailey.neo4k.api

import mu.KLogger

interface Neo4jServiceTestSuiteBase {

    val neo4jImportFolder: String
    val apiTestServer: String
    var neo4jService: Neo4jService
    var testQueries: Neo4jTestQueries
    val logger: KLogger

}
package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.*


object BoltNeo4jServiceFactory {

    fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "bolt://localhost",
                    boltPort = 7987,
                    username = "neo4j",
                    password = "localhost"
            ))
}


class BoltServiceBasicTest : Neo4jServiceBasicTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceMovieTutorialTest : Neo4jServiceMovieTutorialTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceQueryApiTest : Neo4jServiceQueryApiTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceApocTest : Neo4jServiceApocTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceLoadCsvTest : Neo4jServiceLoadCsvTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceApocLoadJdbcTest : Neo4jServiceApocLoadJdbcTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceApocLoadJsonTest : Neo4jServiceApocLoadJsonTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceNorthwindScriptTest : Neo4jServiceNorthwindScriptTest(BoltNeo4jServiceFactory.createNeo4jService())

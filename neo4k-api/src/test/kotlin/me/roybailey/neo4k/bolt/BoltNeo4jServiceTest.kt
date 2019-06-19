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


class BoltServiceBasicTest : BaseNeo4jServiceBasicTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceMovieTutorialTest : BaseNeo4jServiceMovieTutorialTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceQueryApiTest : BaseNeo4jServiceQueryApiTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceApocTest : BaseNeo4jServiceApocTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceLoadCsvTest : BaseNeo4jServiceLoadCsvTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltServiceApocLoadJdbcTest : BaseServiceApocLoadJdbcTest(BoltNeo4jServiceFactory.createNeo4jService())

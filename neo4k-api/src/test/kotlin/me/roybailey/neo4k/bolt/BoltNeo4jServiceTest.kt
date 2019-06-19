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


class BoltApocTest : BaseNeo4jApocTest(BoltNeo4jServiceFactory.createNeo4jService())


class BoltApocJdbcImportTest : BaseApocJdbcImportTest(BoltNeo4jServiceFactory.createNeo4jService())

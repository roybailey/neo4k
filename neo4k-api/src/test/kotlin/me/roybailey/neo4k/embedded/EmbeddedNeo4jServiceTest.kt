package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*


object EmbeddedNeo4jServiceFactory {

    fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "file://./target/neo4j/EmbeddedServiceBasicTest-{timestamp}",
                    boltPort = 7988
            ))
}


class EmbeddedServiceBasicTest : BaseNeo4jServiceBasicTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceMovieTutorialTest : BaseNeo4jServiceMovieTutorialTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceQueryApiTest : BaseNeo4jServiceQueryApiTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedApocTest : BaseNeo4jApocTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedApocJdbcImportTest : BaseApocJdbcImportTest(EmbeddedNeo4jServiceFactory.createNeo4jService())

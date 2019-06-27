package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*


object EmbeddedNeo4jServiceFactory {

    fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "file://./target/neo4j/EmbeddedServiceTest-{timestamp}",
                    boltPort = 7988
            ))
}


class EmbeddedServiceBasicTest : Neo4jServiceBasicTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceMovieTutorialTest : Neo4jServiceMovieTutorialTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceQueryApiTest : Neo4jServiceQueryApiTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceApocTest : Neo4jServiceApocTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceLoadCsvTest : Neo4jServiceLoadCsvTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceApocLoadJdbcTest : Neo4jServiceApocLoadJdbcTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceApocLoadJsonTest : Neo4jServiceApocLoadJsonTest(EmbeddedNeo4jServiceFactory.createNeo4jService())


class EmbeddedServiceNorthwindScriptTest : Neo4jServiceNorthwindScriptTest(EmbeddedNeo4jServiceFactory.createNeo4jService())

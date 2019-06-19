package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.*


object EmbeddedNeo4jServiceFactory {

    fun createNeo4jService(): Neo4jService = Neo4jService.getInstance(
            Neo4jServiceOptions(
                    neo4jUri = "file://./target/neo4j/EmbeddedServiceBasicTest-{timestamp}",
                    boltPort = 7988
            ))
}


class EmbeddedServiceBasicTest : BaseNeo4jServiceBasicTest() {
    override fun createNeo4jService(): Neo4jService = EmbeddedNeo4jServiceFactory.createNeo4jService()
}


class EmbeddedServiceMovieTutorialTest : BaseNeo4jServiceMovieTutorialTest() {
    override fun createNeo4jService(): Neo4jService = EmbeddedNeo4jServiceFactory.createNeo4jService()
}


class EmbeddedServiceQueryApiTest : BaseNeo4jServiceQueryApiTest() {
    override fun createNeo4jService(): Neo4jService = EmbeddedNeo4jServiceFactory.createNeo4jService()
}


class EmbeddedApocTest : BaseNeo4jApocTest() {
    override fun createNeo4jService(): Neo4jService = EmbeddedNeo4jServiceFactory.createNeo4jService()
}


class EmbeddedApocJdbcImportTest : BaseApocJdbcImportTest() {
    override fun createNeo4jService(): Neo4jService = EmbeddedNeo4jServiceFactory.createNeo4jService()
}

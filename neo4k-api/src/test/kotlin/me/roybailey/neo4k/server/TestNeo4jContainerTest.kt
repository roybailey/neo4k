package me.roybailey.neo4k.server

import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceOptions
import me.roybailey.neo4k.bolt.KNeo4jContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
class TestNeo4jContainerTest {


    @Test
    fun testSomethingUsingBolt() {

        // Retrieve the Bolt URL from the container
        val boltUrl = neo4jContainer.boltUrl
        try {
            GraphDatabase.driver(boltUrl, AuthTokens.basic("neo4j", "localhost")).let { driver: Driver ->
                driver.session().let { session ->
                    val one = session.run("RETURN 1", mapOf<String, Any>()).next().get(0).asLong()
                    assertThat(one).isEqualTo(1L)
                }
            }
        } catch (e: Exception) {
            fail(e.message)
        }

    }


    @Test
    fun testNeo4jServiceAccessToTestContainer() {

        Neo4jService.getInstance(
            Neo4jServiceOptions(
                neo4jUri = neo4jContainer.boltUrl,
                boltPort = 0,
                username = "neo4j",
                password = "localhost"
            )
        ).run {
            val total: Long? = this.queryForObject("match (n) return count(n) as total")
            assertThat(total).isEqualTo(0)
        }
    }


    companion object {

        @JvmStatic
        @Container
        val neo4jContainer = KNeo4jContainer().withAdminPassword("localhost")
    }
}

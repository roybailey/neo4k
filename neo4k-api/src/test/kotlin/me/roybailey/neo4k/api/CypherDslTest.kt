package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class CypherDslTest {

    @Test
    fun testSimpleCypherDslBuilders() {

        val cypher = cypher {
            match {
                cypher = "(n:Movie)-[r:ACTOR]-(p:Person) return n,r,p"
            }
        }
        assertThat(cypher).isEqualToIgnoringCase("match (n:Movie)-[r:ACTOR]-(p:Person) return n,r,p")
    }
}
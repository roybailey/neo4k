package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


abstract class BaseNeo4jServiceApocTest(
        final override val neo4jService: Neo4jService,
        val neo4jApoc: Neo4jApoc = Neo4jApoc(neo4jService)
) : BaseNeo4jServiceTest(neo4jService) {


    @Test
    fun loadApocStatic() {

        val name = "loadApocStatic"
        val value = "test"
        // save a static value through apoc query
        neo4jApoc.setStatic(name, value)
        // check the saved static value can be queried
        Assertions.assertThat(neo4jApoc.getStatic(name)).isEqualTo(value)
        // check unknown static values return the default
        Assertions.assertThat(neo4jApoc.getStatic("__unknown__", "hello")).isEqualTo("hello")
    }

}

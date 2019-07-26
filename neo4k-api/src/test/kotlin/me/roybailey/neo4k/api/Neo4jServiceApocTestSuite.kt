package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


interface Neo4jServiceApocTestSuite : Neo4jServiceTestSuiteBase {


    @Test
    fun loadApocStatic() {

        val name = "loadApocStatic"
        val value = "test"
        // save a static value through apoc query
        neo4jService.setStatic(name, value)
        // check the saved static value can be queried
        Assertions.assertThat(neo4jService.getStatic(name)).isEqualTo(value)
        // check unknown static values return the default
        Assertions.assertThat(neo4jService.getStatic("__unknown__", "hello")).isEqualTo("hello")
    }

}

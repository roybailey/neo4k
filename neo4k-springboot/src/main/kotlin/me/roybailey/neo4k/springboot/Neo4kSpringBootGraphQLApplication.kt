package me.roybailey.neo4k.springboot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class Neo4kSpringBootGraphQLApplication

fun main(args: Array<String>) {
    runApplication<Neo4kSpringBootGraphQLApplication>(*args)
}

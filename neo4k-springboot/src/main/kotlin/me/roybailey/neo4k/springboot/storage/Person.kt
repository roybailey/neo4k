package me.roybailey.neo4k.springboot.storage

data class Person(
    val id: Long?,
    val name: String,
    val born: Int
)
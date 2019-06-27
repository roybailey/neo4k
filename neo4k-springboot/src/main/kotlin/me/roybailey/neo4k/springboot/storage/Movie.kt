package me.roybailey.neo4k.springboot.storage

data class Movie(
    val id: Long?,
    val title: String,
    val released: Int,
    val actors: Collection<Person> = emptyList(),
    val directors: Collection<Person> = emptyList()
)

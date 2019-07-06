package me.roybailey.neo4k.springboot.service

import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceRecord
import me.roybailey.neo4k.api.toNeo4j
import me.roybailey.neo4k.springboot.storage.Movie
import me.roybailey.neo4k.springboot.storage.Person
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.lang.RuntimeException


@Component
class MovieService(val neo4jService: Neo4jService) {

    val logger = KotlinLogging.logger {}


    fun movies(): List<Movie> {
        val movies = mutableListOf<Movie>()
        neo4jService.execute("match (m:Movie) return m") { result ->
            while (result.hasNext()) {
                val record = result.next()["m"] as Neo4jServiceRecord
                movies.add(Movie(
                        id = record["id"] as Long,
                        title = record["title"] as String,
                        released = (record["released"] as Long).toInt()
                ))
            }
        }
        return movies.toList()
    }


    fun movie(movieId: String): Movie? {
        var movie: Movie? = null
        neo4jService.execute("match (m:Movie) where id(m) = __id return m".toNeo4j()) { result ->
            while (result.hasNext()) {
                val record = result.next()["m"] as Neo4jServiceRecord
                movie = Movie(
                        id = null,
                        title = record["title"] as String,
                        released = (record["released"] as Long).toInt()
                )
            }
        }
        return movie
    }


    fun movieActors(movie: Movie): List<Person> {
        val actors = mutableListOf<Person>()
        neo4jService.execute("match (m:Movie { title: __title })-[:ACTED_IN]-(a:Person) return a".toNeo4j(),
                mapOf("title" to movie.title)) { result ->
            while (result.hasNext()) {
                val record = result.next()["a"] as Neo4jServiceRecord
                actors.add(Person(
                        id = record["id"] as Long,
                        name = record["name"] as String,
                        born = (record["born"]?.let { if (it is Long) it.toInt() else 0 } ?: 0)
                ))
            }
        }
        return actors.toList()
    }


    fun movieDirectors(movie: Movie): List<Person> {
        val actors = mutableListOf<Person>()
        neo4jService.execute("match (m:Movie { title: __title })-[:DIRECTED]-(a:Person) return a".toNeo4j(),
                mapOf("title" to movie.title)) { result ->
            while (result.hasNext()) {
                val record = result.next()["a"] as Neo4jServiceRecord
                actors.add(Person(
                        id = record["id"] as Long,
                        name = record["name"] as String,
                        born = (record["born"]?.let { (it as Long).toInt() } ?: 0)
                ))
            }
        }
        return actors.toList()
    }


    fun createMovie(newMovie: Movie): Movie {
        var movie: Movie? = null
        neo4jService.execute(
                """
                UNWIND __newMovie as props
                MERGE (m:Movie { title: props.title })
                    ON CREATE SET m.created = timestamp(), m.released = props.released
                    ON MATCH SET m.updated = timestamp(), m.released = props.released
                WITH props, m
                UNWIND props.actors as actor
                MERGE (a:Person { name: actor.name })
                    ON CREATE SET a.created = timestamp(), a += actor
                    ON MATCH SET a.updated = timestamp(), a += actor
                MERGE (a)-[:ACTED_IN]->(m)
                RETURN m
                """.trimMargin().toNeo4j(),
                mapOf("newMovie" to mapOf(
                        "title" to newMovie.title,
                        "released" to newMovie.released,
                        "actors" to newMovie.actors.map { mapOf("name" to it.name, "born" to it.born) }
                ))) { result ->
            while (result.hasNext()) {
                val record = result.next()["m"] as Neo4jServiceRecord
                movie = Movie(
                        id = record["id"] as Long,
                        title = record["title"] as String,
                        released = (record["released"] as Long).toInt()
                )
                logger.info { "New Movie(${movie!!.id}) created/merged for title '${movie!!.title}'" }
            }
        }

        return if (movie != null) movie!! else throw RuntimeException("Movie not created $newMovie")
    }

}

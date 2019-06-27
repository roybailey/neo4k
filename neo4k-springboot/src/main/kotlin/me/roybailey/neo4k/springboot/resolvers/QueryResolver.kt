package me.roybailey.neo4k.springboot.resolvers

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import me.roybailey.neo4k.springboot.service.MovieService
import me.roybailey.neo4k.springboot.service.PersonService
import me.roybailey.neo4k.springboot.storage.Movie
import me.roybailey.neo4k.springboot.storage.MovieOrderBy
import me.roybailey.neo4k.springboot.storage.Person
import org.springframework.stereotype.Component

@Component
class QueryResolver(
        private val movieService: MovieService,
        private val personService: PersonService
) : GraphQLQueryResolver {

    fun people(): Collection<Person> = personService.people()

    fun movies(): Collection<Movie> = movieService.movies()

    fun movieById(movieId: String): Movie? = movieService.movie(movieId)

    fun actors(movie:Movie): Collection<Person> = movieService.movieActors(movie)

    fun moviesBy(orderBy: MovieOrderBy?): List<Movie> =
            when (orderBy) {
                MovieOrderBy.TITLE -> movieService.movies().sortedBy { it.title }
                MovieOrderBy.RELEASED -> movieService.movies().sortedBy { it.released }
                else -> movieService.movies()
            }
}

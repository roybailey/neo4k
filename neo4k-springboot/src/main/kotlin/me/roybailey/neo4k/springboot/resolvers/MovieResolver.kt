package me.roybailey.neo4k.springboot.resolvers

import com.coxautodev.graphql.tools.GraphQLResolver
import me.roybailey.neo4k.springboot.service.MovieService
import me.roybailey.neo4k.springboot.storage.*
import org.springframework.stereotype.Component

@Component
class MovieResolver(
    private val movieService: MovieService
) : GraphQLResolver<Movie> {

    fun actors(movie: Movie): Collection<Person> = movieService.movieActors(movie)

    fun directors(movie: Movie): Collection<Person> = movieService.movieDirectors(movie)
}

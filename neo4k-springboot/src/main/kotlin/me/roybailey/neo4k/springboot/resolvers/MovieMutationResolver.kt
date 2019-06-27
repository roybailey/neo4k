package me.roybailey.neo4k.springboot.resolvers

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import me.roybailey.neo4k.springboot.service.MovieService
import me.roybailey.neo4k.springboot.storage.Movie
import me.roybailey.neo4k.springboot.storage.Person
import org.springframework.stereotype.Component

@Component
class MovieMutationResolver(
    private val movieService: MovieService
) : GraphQLMutationResolver {

    fun createMovie(newMovie: Movie): Movie = movieService.createMovie(newMovie)

}

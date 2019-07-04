package me.roybailey.neo4k.api

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test


abstract class Neo4jServiceBasicTest(override val neo4jService: Neo4jService)
    : BaseNeo4jServiceTest(neo4jService) {

    @Test
    fun `should load movies graph without errors`() {

        testQueries.loadMovieData()

        val movieCount: Long = neo4jService.queryForObject("match (m:Movie) return count(m) as movieCount")!!
        assertThat(movieCount).isNotZero()

        val movieData: List<Map<String, Any>> = neo4jService.query("match (m:Movie)--(p:Person) return m,p") { it.asMap() }
        LOG.info { movieData }
        assertThat(movieData).isNotEmpty

        val expectedNames = listOf(
                "Emil Eifrem",
                "Keanu Reeves",
                "Carrie-Anne Moss",
                "Laurence Fishburne",
                "Hugo Weaving")

        // Neo4jService.execute...
        neo4jService.run {
            val actualNames = mutableListOf<String>()
            execute(
                    "match (m:Movie { title: __title })-[:ACTED_IN]-(p:Person) return m,p".toNeo4j(),
                    mapOf("title" to "The Matrix")) {
                while (it.hasNext()) {
                    LOG.info { it }
                    actualNames.add((it.next()["p"] as Neo4jServiceRecord)["name"] as String)
                }
            }
            SoftAssertions().run {
                assertThat(actualNames.size).isEqualTo(expectedNames.size)
                assertThat(actualNames.toList().sorted()).isEqualTo(expectedNames.sorted())
            }
        }

        // Neo4jService.query...
        neo4jService.run {
            val actualNames = query(
                    "match (m:Movie { title: __title })-[:ACTED_IN]-(p:Person) return m,p".toNeo4j(),
                    mapOf("title" to "The Matrix")) { record ->
                LOG.info { record }
                record.asNode("p")!!.asString("name")!!
            }
            SoftAssertions().run {
                assertThat(actualNames.size).isEqualTo(expectedNames.size)
                assertThat(actualNames.toList().sorted()).isEqualTo(expectedNames.sorted())
            }
        }
    }

    @Test
    fun `should process table style results`() {

        testQueries.loadMovieData()

        val query = Neo4jCypher.toNeo4j("""
                match (m:Movie)
                optional match (m)-[:ACTED_IN]-(actor:Person)
                return m.title as title, m.released as released, collect(actor.name) as actors
        """.trimIndent())

        data class MovieResult(val title: String, val released: Long, val actors: List<String>)

        val records = mutableListOf<MovieResult>()
        val expectedRecords = 38

        // Neo4jService.execute...
        neo4jService.run {
            execute(query) { result ->
                while (result.hasNext()) {
                    result.next()
                            .also { LOG.info { it } }
                            .also {
                                records.add(MovieResult(
                                        it["title"].toString(),
                                        it["released"] as Long,
                                        it["actors"] as List<String>))
                            }
                }
            }
            records.forEach { LOG.info { it } }
            Assertions.assertThat(records.size).isEqualTo(expectedRecords)
        }

        // Neo4jService.query...
        neo4jService.run {
            query(query) { record ->
                LOG.info { record }
                MovieResult(
                        record.asString("title")!!,
                        record.asLong("released", 0L),
                        record.asList("actors"))
            }
            records.forEach { LOG.info { it } }
            Assertions.assertThat(records.size).isEqualTo(expectedRecords)
        }
    }

    @Test
    fun `should process graph style results`() {

        testQueries.loadMovieData()

        val query = "match (m:Movie)-[:DIRECTED]-(d:Person) return m, d"

        data class PersonResult(val id: Long, val name: String, val born: Long)
        data class MovieResult(val id: Long,
                               val title: String,
                               val released: Long,
                               val actors: MutableList<PersonResult>,
                               val directors: MutableList<PersonResult>)

        val mapMovies = mutableMapOf<Long, MovieResult>()
        val mapDirectors = mutableMapOf<Long, PersonResult>()
        val expectedRecords = 38

        // Neo4jService.execute...
        neo4jService.run {
            execute(query) { result ->
                while (result.hasNext()) {
                    result.next()
                            .also { LOG.info { it } }
                            .also { record ->

                                val movie = (record["m"] as Neo4jServiceRecord)
                                val director = (record["d"] as Neo4jServiceRecord)
                                LOG.info { "movieId=${movie["id"]} directorId=${director["id"]}" }
                                LOG.info { "movie.labels=${movie["labels"]} director.labels=${director["labels"]}" }

                                if (!mapDirectors.containsKey(director["id"] as Long))
                                    mapDirectors[director["id"] as Long] = PersonResult(director["id"] as Long,
                                            (director["name"]) as String,
                                            (director["born"]) as Long)

                                if (!mapMovies.containsKey(movie["id"] as Long))
                                    mapMovies[movie["id"] as Long] = MovieResult(movie["id"] as Long,
                                            (movie["title"]) as String,
                                            (movie["released"]) as Long,
                                            mutableListOf(),
                                            mutableListOf())

                                mapMovies[movie["id"] as Long]?.directors?.add(mapDirectors[director["id"] as Long]!!)
                            }
                }
                Assertions.assertThat(mapMovies.size).isEqualTo(expectedRecords)
            }
        }

        // Neo4jService.query...
        neo4jService.run {
            query(query) { record ->
                LOG.info { record }
                val movie = record.asNode("m")!!
                val director = record.asNode("d")!!
                LOG.info { "movieId=${movie.id()} directorId=${director.id()}" }
                LOG.info { "movie.labels=${movie.labels()} director.labels=${director.labels()}" }

                if (!mapDirectors.containsKey(director["id"]))
                    mapDirectors[director.id()] = PersonResult(director.id(),
                            director.asString("name")!!,
                            director.asLong("born", 0L))

                if (!mapMovies.containsKey(movie.id()))
                    mapMovies[movie.id()] = MovieResult(movie.id(),
                            movie.asString("title")!!,
                            movie.asLong("released", 0L),
                            mutableListOf(),
                            mutableListOf())

                mapMovies[movie.id()]?.directors?.add(mapDirectors[director.id()]!!)
            }
            Assertions.assertThat(mapMovies.size).isEqualTo(expectedRecords)
        }
    }
}


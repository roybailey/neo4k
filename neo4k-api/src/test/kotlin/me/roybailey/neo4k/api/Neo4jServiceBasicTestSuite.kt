package me.roybailey.neo4k.api

import me.roybailey.neo4k.dsl.toNeo4j
import mu.KLogger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test


interface Neo4jServiceBasicTestSuite : Neo4jServiceTestSuiteBase {

    @Test
    fun `should load movies graph without errors`() {

        testQueries.loadMovieData()

        val movieCount: Long = neo4jService.queryForObject("match (m:Movie) return count(m) as movieCount")!!
        assertThat(movieCount).isNotZero()

        val movieData: List<Map<String, Any>> =
            neo4jService.query("match (m:Movie)--(p:Person) return m,p") { it.asMap() }
        logger.info { movieData }
        assertThat(movieData).isNotEmpty

        val expectedNames = listOf(
            "Emil Eifrem",
            "Keanu Reeves",
            "Carrie-Anne Moss",
            "Laurence Fishburne",
            "Hugo Weaving"
        )

        // Neo4jService.execute...
        neo4jService.run {
            val actualNames = mutableListOf<String>()
            execute(
                "match (m:Movie { title: __title })-[:ACTED_IN]-(p:Person) return m,p".toNeo4j(),
                mapOf("title" to "The Matrix")
            ) {
                while (it.hasNext()) {
                    logger.info { it }
                    actualNames.add((it.next()["p"] as Neo4jServiceRecord)["name"] as String)
                }
            }
            SoftAssertions().apply {
                assertThat(actualNames.size).isEqualTo(expectedNames.size)
                assertThat(actualNames.toList().sorted()).isEqualTo(expectedNames.sorted())
            }.assertAll()
        }

        // Neo4jService.query...
        neo4jService.run {
            val actualNames = query(
                "match (m:Movie { title: __title })-[:ACTED_IN]-(p:Person) return m,p".toNeo4j(),
                mapOf("title" to "The Matrix")
            ) { record ->
                logger.info { record }
                record.asNode("p")!!.asString("name")!!
            }
            SoftAssertions().apply {
                assertThat(actualNames.size).isEqualTo(expectedNames.size)
                assertThat(actualNames.toList().sorted()).isEqualTo(expectedNames.sorted())
            }.assertAll()
        }
    }

    // create clean data classes
    private data class PersonResult(
        val id: Long? = null,
        val name: String,
        val born: Long = 0,
        val unavailable: String? = null
    )

    private data class MovieResult(
        val id: Long? = null,
        val title: String,
        val released: Long,
        val actors: MutableList<PersonResult> = emptyList<PersonResult>().toMutableList(),
        val directors: MutableList<PersonResult> = emptyList<PersonResult>().toMutableList(),
        val expectingNull: String? = null
    )

    // extend neo4j record with mapping functions for our data classes
    private fun Neo4jServiceRecord.toMovieResult(m: String? = null): MovieResult {
        val movieNode = m?.let { asNode(it) } ?: this
        return MovieResult(
            id = movieNode.id(),
            title = movieNode.asString("title")!!,
            released = movieNode.asLong("released"),
            actors = movieNode.asList<PersonResult>("actors").toMutableList(),
            directors = movieNode.asList<PersonResult>("directors").toMutableList(),
            expectingNull = movieNode.asString("expectingNull")
        )
    }


    @Test
    fun `should process table style results`() {

        testQueries.loadMovieData()

        val query = """
                match (m:Movie)
                optional match (m)-[:ACTED_IN]-(actor:Person)
                return m.title as title, m.released as released, collect(actor.name) as actors
        """.toNeo4j().trimIndent()

        val expectedRecords = 38

        // Neo4jService.execute...
        neo4jService.run {
            val records = mutableListOf<MovieResult>()
            execute(query) { result ->
                while (result.hasNext()) {
                    result.next()
                        .also { logger.info { it } }
                        .also {
                            records.add(
                                MovieResult(
                                    title = it["title"].toString(),
                                    released = it["released"] as Long,
                                    actors = (it["actors"] as List<String>).map { PersonResult(name = it) }.toMutableList(),
                                    expectingNull = it["expectingNull"]?.toString()
                                )
                            )
                        }
                }
            }
            assertThat(records.size).isEqualTo(expectedRecords)
            SoftAssertions().apply {
                records.forEach {
                    logger.info { it }
                    assertThat(it.id == null || it.id == 0L).isTrue
                    assertThat(it.title).isNotNull
                    assertThat(it.released).isGreaterThan(0L)
                    assertThat(it.actors).isNotEmpty
                    assertThat(it.expectingNull).isNull()
                }
            }.assertAll()
        }

        // Neo4jService.query...
        neo4jService.run {
            val records = query(query) { record ->
                logger.info { record }
                record.toMovieResult()
            }
            assertThat(records.size).isEqualTo(expectedRecords)
            SoftAssertions().apply {
                records.forEach {
                    logger.info { it }
                    assertThat(it.id == null || it.id == 0L).isTrue
                    assertThat(it.title).isNotNull
                    assertThat(it.released).isGreaterThan(0L)
                    assertThat(it.actors).isNotEmpty
                    assertThat(it.expectingNull).isNull()
                }
            }.assertAll()
        }
    }

    @Test
    fun `should process graph style results`() {

        testQueries.loadMovieData()

        val query = "match (m:Movie)-[:DIRECTED]-(d:Person) return m, d"

        val mapMovies = mutableMapOf<Long, MovieResult>()
        val mapDirectors = mutableMapOf<Long, PersonResult>()
        val expectedRecords = 38

        // Neo4jService.execute...
        neo4jService.run {
            execute(query) { result ->
                while (result.hasNext()) {
                    result.next()
                        .also { logger.info { it } }
                        .also { record ->

                            val movie = (record["m"] as Neo4jServiceRecord)
                            val director = (record["d"] as Neo4jServiceRecord)
                            logger.info { "movieId=${movie["id"]} directorId=${director["id"]}" }
                            logger.info { "movie.labels=${movie["labels"]} director.labels=${director["labels"]}" }

                            if (!mapDirectors.containsKey(director["id"] as Long))
                                mapDirectors[director["id"] as Long] = PersonResult(
                                    director["id"] as Long,
                                    (director["name"]) as String,
                                    (director["born"]) as Long
                                )

                            if (!mapMovies.containsKey(movie["id"] as Long))
                                mapMovies[movie["id"] as Long] = MovieResult(
                                    movie["id"] as Long,
                                    (movie["title"]) as String,
                                    (movie["released"]) as Long,
                                    mutableListOf(),
                                    mutableListOf()
                                )

                            mapMovies[movie["id"] as Long]?.directors?.add(mapDirectors[director["id"] as Long]!!)
                        }
                }
                assertThat(mapMovies.size).isEqualTo(expectedRecords)
            }
        }

        // Neo4jService.query...
        neo4jService.run {
            query(query) { record ->
                logger.info { record }
                val movie = record.asNode("m")!!
                val director = record.asNode("d")!!
                logger.info { "movieId=${movie.id()} directorId=${director.id()}" }
                logger.info { "movie.labels=${movie.labels()} director.labels=${director.labels()}" }

                if (!mapDirectors.containsKey(director.id()))
                    mapDirectors[director.id()] = PersonResult(
                        director.id(),
                        director.asString("name")!!,
                        director.asLong("born", 0L)
                    )

                if (!mapMovies.containsKey(movie.id()))
                    mapMovies[movie.id()] = MovieResult(
                        movie.id(),
                        movie.asString("title")!!,
                        movie.asLong("released", 0L),
                        mutableListOf(),
                        mutableListOf()
                    )

                mapMovies[movie.id()]?.directors?.add(mapDirectors[director.id()]!!)
            }
            assertThat(mapMovies.size).isEqualTo(expectedRecords)
        }
    }
}


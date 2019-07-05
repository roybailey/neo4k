package me.roybailey.neo4k.api

import me.roybailey.neo4k.Neo4jServiceTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


/**
 * These tests use queries from the Neo4j `play: movies` tutorial
 */
abstract class Neo4jServiceMovieTutorialTest(final override val neo4jService: Neo4jService)
    : Neo4jServiceTestBase(neo4jService) {


    @Test
    fun `should execute movie tutorial queries`() {

        testQueries.loadMovieData()
        testMovieTutorialFindExamples()
        testMovieTutorialQueryExamples()
        testMovieTutorialSolveExamples()
    }


    private fun testMovieTutorialFindExamples() {

        "MATCH (tom {name: 'Tom Hanks'}) RETURN tom".run {
            neo4jService.execute(this) {
                assertThat((it.single()["tom"] as Neo4jServiceRecord).asMap()["name"]).isEqualTo("Tom Hanks")
            }
        }

        "MATCH (cloudAtlas {title: 'Cloud Atlas'}) RETURN cloudAtlas".run {
            neo4jService.execute(this) {
                assertThat((it.single()["cloudAtlas"] as Neo4jServiceRecord).asMap()["title"]).isEqualTo("Cloud Atlas")
            }
        }

        "MATCH (people:Person) RETURN people.name LIMIT 10".run {
            neo4jService.execute(this) {
                val people = it.list()
                assertThat(people).hasSize(10)
                people.forEach { person ->
                    assertThat(person.containsKey("name"))
                }
            }
        }

        """ MATCH (nineties:Movie)
            WHERE nineties.released >= 1990 AND nineties.released < 2000
            RETURN nineties.title""".trimIndent().run {
            neo4jService.execute(this) {
                val titles = it.list()
                assertThat(titles).hasSize(20)
                titles.forEach { title ->
                    assertThat(title.containsKey("title"))
                }
            }
        }
    }


    private fun testMovieTutorialQueryExamples() {

        """ MATCH (tom:Person {name: "Tom Hanks"})-[:ACTED_IN]->(tomHanksMovies)
            RETURN tom,tomHanksMovies""".trimMargin().run {
            neo4jService.execute(this) {
                val movies = it.list()
                assertThat(movies).hasSize(12)
                movies.forEach { record ->
                    assertThat(record.containsKey("tom"))
                    assertThat(record.containsKey("tomHanksMovies"))
                }
            }
        }

        """ MATCH (cloudAtlas {title: "Cloud Atlas"})<-[:DIRECTED]-(directors)
            RETURN directors.name""".trimMargin().run {
            neo4jService.execute(this) {
                val directors = it.list()
                assertThat(directors).hasSize(3)
            }
        }

        """ MATCH (tom:Person {name:"Tom Hanks"})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors)
            RETURN coActors.name""".trimMargin().run {
            neo4jService.execute(this) {
                val coactors = it.list()
                assertThat(coactors).hasSize(39)
            }
        }

        """ MATCH (people:Person)-[relatedTo]-(:Movie {title: "Cloud Atlas"})
            RETURN people.name, Type(relatedTo), relatedTo""".trimMargin().run {
            neo4jService.execute(this) {
                val records = it.list()
                assertThat(records).hasSize(10)
                records.forEach { record ->
                    assertThat(record["people.name"]).isNotNull()
                    assertThat(record["Type(relatedTo)"]).isNotNull()
                    assertThat(record["relatedTo"]).isNotNull()
                    // todo support relationships
//                if(record["people.name"]?.equals("Jessica Thompson")!!) {
//                    val relatedTo = record["relatedTo"] as Neo4jServiceRecord
//                    assertThat(relatedTo.asMap()).containsKeys("summary", "rating")
//                }
                }
            }
        }
    }


    private fun testMovieTutorialSolveExamples() {

        """ MATCH (bacon:Person {name:"Kevin Bacon"})-[*1..4]-(hollywood)
            RETURN DISTINCT hollywood""".trimIndent().run {
            neo4jService.execute(this) {
                val movies = it.list()
                assertThat(movies).hasSize(135)
            }
        }

        """ MATCH p=shortestPath(
           (bacon:Person {name:"Kevin Bacon"})-[*]-(meg:Person {name:"Meg Ryan"})
         )
         RETURN p""".trimIndent().run {
            neo4jService.execute(this) {
                val directors = it.list()
                assertThat(directors).hasSize(1)
            }
        }

    }

}

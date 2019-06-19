package me.roybailey.neo4k.api

import me.roybailey.neo4k.util.MarkdownProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


/**
 * These tests use queries from the Neo4j `play: movies` tutorial
 */
abstract class BaseNeo4jServiceMovieTutorialTest(final override val neo4jService: Neo4jService)
    : BaseNeo4jServiceTest(neo4jService) {


    @Test
    fun `should load and execute movie tutorial queries`() {

        testQueries.loadMovieData()

        val queries = MarkdownProperties.loadFromClasspath("/queries/movie-tutorial.adoc", this)
        queries.entries.forEachIndexed { index, query -> LOG.info { "$index) key=${query.key} value=${query.value}" } }
        assertThat(queries).hasSize(3)

        val statements = QueryStatement.parseQueryScriptStatements(queries)
        statements.entries.forEachIndexed { index, entry -> LOG.info { "$index) key=${entry.key} query=${entry.value}" } }

        testMovieTutorialFindExamples(statements.getOrElse("Find") { emptyList() })
        testMovieTutorialQueryExamples(statements.getOrElse("Query") { emptyList() })
        testMovieTutorialSolveExamples(statements.getOrElse("Solve") { emptyList() })
    }


    private fun testMovieTutorialFindExamples(examples: List<QueryStatement>) {

        assertThat(examples).hasSize(4)

        // MATCH (tom {name: "Tom Hanks"}) RETURN tom
        neo4jService.execute(examples[0].query) {
            assertThat((it.single()["tom"] as Neo4jServiceRecord).asMap()["name"]).isEqualTo("Tom Hanks")
        }

        // MATCH (cloudAtlas {title: "Cloud Atlas"}) RETURN cloudAtlas
        neo4jService.execute(examples[1].query) {
            assertThat((it.single()["cloudAtlas"] as Neo4jServiceRecord).asMap()["title"]).isEqualTo("Cloud Atlas")
        }

        // MATCH (people:Person) RETURN people.name LIMIT 10
        neo4jService.execute(examples[2].query) {
            val people = it.list()
            assertThat(people).hasSize(10)
            people.forEach { person ->
                assertThat(person.containsKey("name"))
            }
        }

        // MATCH (nineties:Movie) WHERE nineties.released >= 1990 AND nineties.released < 2000 RETURN nineties.title
        neo4jService.execute(examples[3].query) {
            val titles = it.list()
            assertThat(titles).hasSize(20)
            titles.forEach { title ->
                assertThat(title.containsKey("title"))
            }
        }
    }


    private fun testMovieTutorialQueryExamples(examples: List<QueryStatement>) {

        assertThat(examples).hasSize(4)
        val listOfTomHanksMovies = examples[0]
        val whoDirectedCloudAtlas = examples[1]
        val tomHanksCoActors = examples[2]
        val peopleRelatedToCloudAtlas = examples[3]

        // MATCH (tom:Person {name: "Tom Hanks"})-[:ACTED_IN]->(tomHanksMovies) RETURN tom,tomHanksMovies
        neo4jService.execute(listOfTomHanksMovies.query) {
            val movies = it.list()
            assertThat(movies).hasSize(12)
            movies.forEach { record ->
                assertThat(record.containsKey("tom"))
                assertThat(record.containsKey("tomHanksMovies"))
            }
        }

        // MATCH (cloudAtlas {title: "Cloud Atlas"})<-[:DIRECTED]-(directors) RETURN directors.name
        neo4jService.execute(whoDirectedCloudAtlas.query) {
            val directors = it.list()
            assertThat(directors).hasSize(3)
        }

        // MATCH (tom:Person {name:"Tom Hanks"})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors) RETURN coActors.name
        neo4jService.execute(tomHanksCoActors.query) {
            val coactors = it.list()
            assertThat(coactors).hasSize(39)
        }

        // MATCH (people:Person)-[relatedTo]-(:Movie {title: "Cloud Atlas"}) RETURN people.name, Type(relatedTo), relatedTo
        neo4jService.execute(peopleRelatedToCloudAtlas.query) {
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


    private fun testMovieTutorialSolveExamples(examples: List<QueryStatement>) {

        assertThat(examples).hasSize(2)
        val moviesActorsFourHopsFromKevinBacon = examples[0]
        val baconShortestPathToMegRyan = examples[1]

        // MATCH (bacon:Person {name:"Kevin Bacon"})-[*1..4]-(hollywood)
        // RETURN DISTINCT hollywood
        neo4jService.execute(moviesActorsFourHopsFromKevinBacon.query) {
            val movies = it.list()
            assertThat(movies).hasSize(135)
        }

        // MATCH p=shortestPath(
        //   (bacon:Person {name:"Kevin Bacon"})-[*]-(meg:Person {name:"Meg Ryan"})
        // )
        // RETURN p
        neo4jService.execute(baconShortestPathToMegRyan.query) {
            val directors = it.list()
            assertThat(directors).hasSize(1)
        }

    }

}

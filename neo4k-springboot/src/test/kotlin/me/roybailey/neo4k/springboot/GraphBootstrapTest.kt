package me.roybailey.neo4k.springboot

import junit.framework.TestCase.assertTrue
import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.springboot.config.ApplicationBootstrap
import me.roybailey.neo4k.springboot.config.GraphBootstrap
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean


class GraphBootstrapTest : AbstractIntegrationTest() {

	// prevent the ApplicationBootstrap from triggering bootstrap steps
	@MockBean
	lateinit var appBootstrap: ApplicationBootstrap

	@Autowired
	lateinit var graphBootstrap: GraphBootstrap

	@Autowired
	lateinit var neo4jService: Neo4jService

	@Test
	fun contextLoads() {

		val countBefore:Long = neo4jService.queryForObject("match (n) return count(n)", mutableMapOf())!!
		LOG.info { "graph count before bootstrap = $countBefore" }

		graphBootstrap.initializeGraph()

		val countAfter:Long = neo4jService.queryForObject("match (n) return count(n)", mutableMapOf())!!
		LOG.info { "graph count after bootstrap = $countAfter" }
		assertTrue(countAfter > countBefore)
	}

}

package me.roybailey.neo4k.springboot

import org.junit.Test


class ApplicationTest : AbstractIntegrationTest() {

	@Test
	fun contextLoads() {
		LOG.info { "Application started without exceptions" }
	}

}

package me.roybailey.neo4k.api

import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo


abstract class BaseTest {

    val LOG = KotlinLogging.logger(this.javaClass.name)

    @BeforeEach
    fun setupBase(testInfo: TestInfo) {
        LOG.info { "Before ${testInfo.testClass}.${testInfo.testMethod}" }
    }

    @AfterEach
    fun shutdownBase(testInfo: TestInfo) {
        LOG.info { "After ${testInfo.testClass.get()}.${testInfo.testMethod.get()}" }
    }

}
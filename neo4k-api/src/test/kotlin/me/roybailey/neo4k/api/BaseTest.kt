package me.roybailey.neo4k.api

import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.nio.file.Paths
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.TimeUnit


abstract class BaseTest {

    val LOG = KotlinLogging.logger(this.javaClass.name)

    var started: Instant = now()
    var projectFolder = "."
    var testDataFolder = "."


    @BeforeEach
    fun setupBase(testInfo: TestInfo) {
        LOG.info("************************************************************")
        LOG.info { "Running ${testInfo.testClass.get().name}.`${testInfo.testMethod.get().name}`" }
        LOG.info("************************************************************")
        started = now()
    }


    @AfterEach
    fun shutdownBase(testInfo: TestInfo) {
        val elapsed = now().minusMillis(started.toEpochMilli()).toEpochMilli()
        LOG.info { "------------------------------------------------------------" }
        LOG.info { "Finished ${testInfo.testClass.get().name}.`${testInfo.testMethod.get().name}` [$elapsed]" }
        LOG.info { "============================================================" }
        LOG.info { "\n\n\n" }
    }


    @BeforeEach
    fun findTestDataFolder() {

        // resolve project root folder so it works locally and on TeamCity
        projectFolder = Paths.get("").toAbsolutePath().toString()
        LOG.info { "Searching folders from ${Paths.get(projectFolder).toAbsolutePath()}" }

        if(projectFolder.endsWith("neo4k-api"))
            projectFolder = Paths.get(projectFolder+"/..").toAbsolutePath().toString()

        projectFolder = projectFolder.replace("\\", "/")

        if (!projectFolder.endsWith("/"))
            projectFolder += "/"

        LOG.info { "Using projectFolder from $projectFolder" }

        testDataFolder = projectFolder + "neo4k-api/src/test/resources/testdata"

        LOG.info { "Using testDataFolder from $testDataFolder" }
    }
}

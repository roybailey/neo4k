package me.roybailey.neo4k

import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.nio.file.Paths
import java.time.Instant
import java.time.Instant.now


abstract class UnitTestBase {

    val logger = KotlinLogging.logger(this.javaClass.name)

    var started: Instant = now()
    var projectFolder = "."
    var moduleFolder = "."
    var testDataFolder = "."


    @BeforeEach
    fun setupBase(testInfo: TestInfo) {
        logger.info("************************************************************")
        logger.info { "Running ${testInfo.testClass.get().name}.`${testInfo.testMethod.get().name}`" }
        logger.info("************************************************************")
        started = now()
    }


    @AfterEach
    fun shutdownBase(testInfo: TestInfo) {
        val elapsed = now().minusMillis(started.toEpochMilli()).toEpochMilli()
        logger.info { "------------------------------------------------------------" }
        logger.info { "Finished ${testInfo.testClass.get().name}.`${testInfo.testMethod.get().name}` [$elapsed]" }
        logger.info { "============================================================" }
        logger.info { "\n\n\n" }
    }


    @BeforeEach
    fun findTestDataFolder() {

        // resolve project root folder so it works locally and on TeamCity
        var cwd = Paths.get("").toAbsolutePath().toString().replace("\\", "/")
        if (!cwd.endsWith("/"))
            cwd += "/"
        logger.info { "Searching folders from ${Paths.get(projectFolder).toAbsolutePath()}" }

        val rootFolder = "neo4k"
        projectFolder = cwd.substring(0, cwd.indexOf(rootFolder) + rootFolder.length)
        moduleFolder = cwd.substring(0, cwd.indexOf("/", projectFolder.length+1))
        testDataFolder = "$moduleFolder/src/test/resources/testdata"

        logger.info { "Using projectFolder from $projectFolder" }
        logger.info { "Using ModuleFolder from $moduleFolder" }
        logger.info { "Using testDataFolder from $testDataFolder" }
    }

}

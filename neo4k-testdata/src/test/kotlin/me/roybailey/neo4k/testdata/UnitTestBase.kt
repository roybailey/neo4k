package me.roybailey.neo4k.testdata

import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.time.Instant.now


/**
 * A standard Unit test base class to implement some common features.
 *
 * Note: Resolving build execution folder locations for accessing test data files from file-system
 * is required to test Neo4j cypher can load from the file-system correctly.
 */
abstract class UnitTestBase {

    val logger = KotlinLogging.logger(this.javaClass.name)

    var started: Instant = now()

    companion object {

        val logger = KotlinLogging.logger(UnitTestBase::class.java.name)

        private val cwd = Paths.get("").toAbsolutePath().toString().replace("\\", "/").let {
            when {
                it.endsWith("/") -> it
                else -> "$it/"
            }
        }.also {
            logger.info { "Using cwd from $it" }
        }
        private val gitRootFolder = File(".").let { executionFolder ->
            var pwd: File? = executionFolder
            var found = false
            while (!found && pwd != null) {
                logger.info { "Searching for project root folder...${pwd!!.absolutePath}" }
                found = pwd.listFiles().find {
                    it.isDirectory && it.name == ".git"
                } != null
                if (!found)
                    pwd = pwd.absoluteFile.parentFile
            }
            if (!found)
                throw RuntimeException("""
                    ********** FATAL **********
                    Unable to resolve the project root folder
                    (by searching upwards from ${executionFolder.absolutePath} for .git folder)
                    this is needed to resolve test file paths, for local builds and on build servers
                    ***************************
                    """.trimIndent())
            pwd
        }.also {
            logger.info { "Using gitRootFolder from $it" }
        }

        // Resolved build execution folder locations for accessing test data files from file-system.
        // Required to test Neo4j cypher can load from the file-system correctly.
        // Statically created so that TestContainers can be created with file-system mappings.

        var projectFolder = gitRootFolder!!.absolutePath.also {
            logger.info { "Using projectFolder from $it" }
        }
        var moduleFolder = cwd.substring(0, cwd.indexOf("/", projectFolder.length + 1)).also {
            logger.info { "Using moduleFolder from $it" }
        }
        var projectTestDataFolder = "$projectFolder/testdata".also {
            logger.info { "Using projectTestDataFolder from $it" }
        }
        var moduleTestDataFolder = "$moduleFolder/src/test/resources/testdata".also {
            logger.info { "Using moduleTestDataFolder from $it" }
        }
    }


    @BeforeEach
    fun setupBase(testInfo: TestInfo) {
        started = now()
        logger.info("************************************************************")
        logger.info { "Running ${testInfo.testClass.get().name}.`${testInfo.testMethod.get().name}`" }
        logger.info("************************************************************")
    }


    @AfterEach
    fun shutdownBase(testInfo: TestInfo) {
        val elapsed = now().minusMillis(started.toEpochMilli()).toEpochMilli()
        logger.info { "------------------------------------------------------------" }
        logger.info { "Finished ${testInfo.testClass.get().name}.`${testInfo.testMethod.get().name}` [${elapsed / 1000} seconds]" }
        logger.info { "============================================================" }
        logger.info { "\n\n\n" }
    }

}

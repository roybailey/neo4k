package me.roybailey.neo4k.testdata

import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileWriter


class GenerateTestDataTest : UnitTestBase() {

    @Test
    fun `generate test data`() {

        val generatorFolder = "$projectFolder/testdata/generator"
        File(generatorFolder).mkdir()

        val writer = FileWriter("$generatorFolder/sample.txt")
        writer.write("hello")
        writer.flush()
        writer.close()
    }
}
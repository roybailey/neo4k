package me.roybailey.neo4k.reporting

import me.roybailey.neo4k.testdata.UnitTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter


class ReportQueryAsciiDocTest : UnitTestBase() {


    @Test
    fun `Test Report Query AsciiDoc Generation`(testInfo: TestInfo) {

        val file = File(moduleFolder, "target/_report_query.adoc")

        logger.info("${testInfo.displayName} output to ${file.absolutePath}")
        PrintWriter(FileWriter(file)).use { output ->

            output.println("""
            = Reporting Service - Report Query Library =

            > Auto-generated from code.  DO NOT EDIT

            :toc:
            :toc-placement!:
            :toc-title: TABLE OF CONTENTS
            :toclevels: 2

            toc::[]
            """.trimIndent())

            // using our own simple template to get trimIndent() to work correctly
            // (otherwise the mix of indented text here and un-indented text from the kotlin template doesn't output desired result)
            val queryMarkdownTemplate = """
            === @REPORT_NAME@

            @REPORT_DESCRIPTION@

            ```
            @REPORT_QUERY@
            ```

            """.trimIndent()

            ReportExamples.values().forEach {
                val queryMarkdown = queryMarkdownTemplate
                        .replace("@REPORT_NAME@", it.name)
                        .replace("@REPORT_DESCRIPTION@", it.report.query.description)
                        .replace("@REPORT_QUERY@", it.report.query.query)
                output.println()
                output.write(queryMarkdown.trimIndent())
                output.println()
            }

            output.flush()
            output.close()
        }
    }

}

package me.roybailey.neo4k.dsl

import java.io.PrintWriter
import java.io.Writer


// ------------------------------------------------------------
// DSL Syntax Sugar root functions
// ------------------------------------------------------------

object CypherDocumentation {


    fun toAsciiDocTableOfContents(writer: Writer, title: String) {
        PrintWriter(writer).let { output ->

            output.println("""
                = $title =

                > Auto-generated from code.  DO NOT EDIT

                :toc:
                :toc-placement!:
                :toc-title: TABLE OF CONTENTS
                :toclevels: 2

                toc::[]
                """.trimIndent())
        }
    }


    fun toAsciiDoc(library: ScriptLibrary, writer: Writer, title: String) {

        toAsciiDocTableOfContents(writer, title)

        PrintWriter(writer).let { output ->

            // using our own simple template to get trimIndent() to work correctly
            // (otherwise the mix of indented text here and un-indented text from the kotlin template doesn't output desired result)
            val scriptMarkdownTemplate = """
            === @SCRIPT_NAME@


            ```
            @STATEMENT_LIST@
            ```

            """.trimIndent()

            val queryMarkdownTemplate = """

            // @STATEMENT_DESCRIPTION@
            // @STATEMENT_DEFAULT_PARAMETERS@
            @STATEMENT_QUERY@

            """.trimIndent()

            library.entries.forEach { entry ->
                val script = StringBuffer()
                entry.value.forEach { queryStatement ->
                    script.append(queryMarkdownTemplate
                            .replace("@STATEMENT_DESCRIPTION@", queryStatement.description)
                            .replace("@STATEMENT_DEFAULT_PARAMETERS@", queryStatement.defaultParams.toString())
                            .replace("@STATEMENT_QUERY@", queryStatement.query))
                }
                val scriptMarkdown = scriptMarkdownTemplate
                        .replace("@SCRIPT_NAME@", entry.key)
                        .replace("@STATEMENT_LIST@", script.toString())
                output.println()
                output.write(scriptMarkdown.trimIndent())
                output.println()
            }

            output.flush()
            output.close()
        }
    }


    fun toAsciiDoc(writer: Writer, title: String, code: String, cypher: String) {
        PrintWriter(writer).let { output ->

            // using our own simple template to get trimIndent() to work correctly
            // (otherwise the mix of indented text here and un-indented text from the kotlin template doesn't output desired result)
            val sampleMarkdownTemplate = """


            === @TITLE@

            ```
            @CODE@
            ```
            ~produces...~
            ```
            @CYPHER@
            ```

            """.trimIndent()

            output.append(sampleMarkdownTemplate
                    .replace("@TITLE@", title)
                    .replace("@CODE@", code)
                    .replace("@CYPHER@", cypher))

            output.flush()
        }
    }

}

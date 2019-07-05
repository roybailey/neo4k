package me.roybailey.neo4k.api

import java.io.PrintWriter
import java.io.Writer


// ------------------------------------------------------------
// DSL Syntax Sugar root functions
// ------------------------------------------------------------

typealias ScriptLibrary = MutableMap<String, MutableList<QueryStatement>>

object ScriptDsl {

    private fun trimAndJoin(vararg args: String) = args.map { it.trimIndent() }.joinToString(separator = "\n") { it.trimIndent() }

    private fun parameters(minimum: Int, vararg args: String): String {
        val params = mutableListOf<String>()
        args.take(minimum).forEach { params += it }
        args.slice(minimum..args.size).forEach { if (!it.isNullOrBlank()) params += it }
        return params.joinToString(",")
    }

    fun quote(text: String, doubleQuotes: Boolean = true, quoted: Boolean = true) =
            when (quoted) {
                true -> when (doubleQuotes) {
                    true -> "\"$text\""
                    else -> "\'$text\'"
                }
                else -> text
            }

    //
    // Root DSL starter methods
    //

    fun scriptLibrary(init: ScriptDefinitionContext.() -> Unit): ScriptLibrary {
        val context = ScriptDefinitionContext().apply(init)
        return context.build()
    }

    fun apocLoadJson(init: ApocLoadJsonContext.() -> Unit): String {
        val context = ApocLoadJsonContext().apply(init)
        return context.build()
    }

    // ------------------------------------------------------------
    // DSL Context objects work like builders
    // ------------------------------------------------------------

    class ScriptDefinitionContext {

        private val scriptLibrary = mutableMapOf<String, MutableList<QueryStatement>>()
        private var name: String? = null

        fun script(name: String, init: QueryStatementContext.() -> Unit) {
            this.scriptLibrary[name] = mutableListOf()
            this.name = name
        }

        fun statement(init: QueryStatementContext.() -> Unit) {
            val context = QueryStatementContext().apply(init)
            val statements = this.scriptLibrary[this.name]!!
            statements.add(context.build("$name - part ${statements.size + 1}"))
        }

        fun build(): ScriptLibrary = scriptLibrary
    }


    class QueryStatementContext {

        var description: String? = null
        var query: String? = null
        var defaultParams = emptyQueryParams()

        fun cypher(init: TopLevelQueryContext .() -> Unit) {
            val context = TopLevelQueryContext().apply(init)
            query = context.build()
        }

        fun build(defaultDescription: String?): QueryStatement =
                SimpleQueryStatement(
                        if (description == null) defaultDescription!! else description!!,
                        query!!,
                        defaultParams
                )
    }


    open class QueryContext {

        var query = StringBuffer()

        fun append(cypher: String) = query.append("$cypher\n")

        fun build(): String = query.toString()
    }


    class TopLevelQueryContext : QueryContext() {

        fun apocLoadJson(init: ApocLoadJsonContext.() -> Unit) {
            val context = ApocLoadJsonContext().apply(init)
            query.append(context.build())
        }

        fun apocPeriodicIterate(init: ApocPeriodicIterateContext.() -> Unit) {
            val context = ApocPeriodicIterateContext().apply(init)
            query.append(context.build())
        }
    }


    class ApocLoadJsonContext {

        var url: String = ""
        var headers = mutableMapOf<String, String>()
        var payload: String? = null
        var path: String? = null
        var cypher: String? = "PROCESS_NOT_ASSIGNED"

        fun cypher(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.cypher = context.build()
        }

        private fun params(): String {
            val params: MutableList<String?> = mutableListOf(
                    url,
                    "{" + headers.entries.map { it.key + ": " + it.value }.joinToString() + "}",
                    payload)
            arrayOf(path).forEach { if (!it.isNullOrBlank()) params += it }
            return params.joinToString(",")
        }

        fun build(): String = trimAndJoin(
                "CALL apoc.load.jsonParams(${params()}) YIELD value WITH value",
                cypher!!
        )
    }


    class ApocPeriodicIterateContext {

        var batchSize: Int = 1000
        var parallel: Boolean = false
        var outer: String = "PERIODIC_OUTER_NOT_ASSIGNED"
        var inner: String = "PERIODIC_INNER_NOT_ASSIGNED"

        fun outer(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.outer = context.build()
        }

        fun inner(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.inner = context.build()
        }

        fun build(): String = arrayOf(
                "CALL apoc.periodic.iterate(\"",
                outer.prependIndent("    "),
                "\",\"",
                inner.prependIndent("    "),
                "\", {batchSize:$batchSize, parallel:$parallel})"
        ).joinToString("\n")
    }


    // ------------------------------------------------------------
    // utilities
    // ------------------------------------------------------------

    fun toAsciiDoc(library: ScriptLibrary, writer: Writer, title: String) {
        PrintWriter(writer).use { output ->

            output.println("""
            = $title =

            > Auto-generated from code.  DO NOT EDIT

            :toc:
            :toc-placement!:
            :toc-title: TABLE OF CONTENTS
            :toclevels: 2

            toc::[]
            """.trimIndent())

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
}

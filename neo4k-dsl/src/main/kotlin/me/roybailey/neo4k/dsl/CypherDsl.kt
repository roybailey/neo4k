package me.roybailey.neo4k.dsl

import java.io.PrintWriter
import java.io.Writer


// ------------------------------------------------------------
// DSL Syntax Sugar root functions
// ------------------------------------------------------------

typealias ScriptLibrary = MutableMap<String, MutableList<QueryStatement>>

fun String.toNeo4j(dollar: String = "__"): String = replace(dollar, "$")
fun String.escapeDoubleQuotes() = this.replace("\"", "\\\"")
fun String.quoted(doubleQuotes: Boolean = true, quoted: Boolean = true) =
        when (quoted) {
            true -> when (doubleQuotes) {
                true -> "\"$this\""
                else -> "\'$this\'"
            }
            else -> this
        }


object CypherDsl {

    private fun trimAndJoin(vararg args: String) = args.map { it.trimIndent() }.joinToString(separator = "\n") { it.trimIndent() }

    private fun parameters(minimum: Int, vararg args: String): String {
        val params = mutableListOf<String>()
        args.take(minimum).forEach { params += it }
        args.slice(minimum..args.size).forEach { if (!it.isNullOrBlank()) params += it }
        return params.joinToString(",")
    }

    // ------------------------------------------------------------
    // Standard Queries
    // ------------------------------------------------------------

    /**
     * Matches all nodes and relationsips and deletes everything from the database.
     * !!!warning!!! DELETES ALL DATA
     */
    fun cypherMatchAndDeleteAll() = "match (n) optional match (n)-[r]-() delete r,n"

    /**
     * Matches all nodes with specific label and deletes them along with their relationships to anything else.
     * !!!warning!!! deletes all data for given label
     * @param label to match
     */
    fun cypherMatchLabelAndDeleteAll(label: String) = "match (n:$label) optional match (n)-[r]-() delete r,n"

    /**
     * Matches all labels and counts their use.
     * @return 'label' (name) and 'total' (count)
     */
    fun cypherMatchDistinctLabelCount() = """
            MATCH (a) WITH DISTINCT LABELS(a) AS temp, COUNT(a) AS tempCnt
            UNWIND temp AS label
            RETURN label, SUM(tempCnt) AS total
            ORDER BY label
            """.trimIndent()

    // ------------------------------------------------------------
    // Root DSL starter methods
    // ------------------------------------------------------------

    fun scriptLibrary(init: ScriptDefinitionContext.() -> Unit): ScriptLibrary {
        val context = ScriptDefinitionContext().apply(init)
        return context.build()
    }

    fun apocLoadJson(init: ApocLoadJsonContext.() -> Unit): String {
        val context = ApocLoadJsonContext().apply(init)
        return context.build()
    }

    fun apocLoadJdbc(init: ApocLoadJdbcContext.() -> Unit): String {
        val context = ApocLoadJdbcContext().apply(init)
        return context.build()
    }

    fun apocPeriodicIterate(init: ApocPeriodicIterateContext.() -> Unit): String {
        val context = ApocPeriodicIterateContext().apply(init)
        return context.build()
    }

    fun loadCsvWithHeaders(fileUrl: String, variable: String = "row", init: LoadCsvContext.() -> Unit): String {
        val context = LoadCsvContext(fileUrl, variable).apply(init)
        return context.build()
    }

    /**
     * Sets a static value
     *
     * @param name - the name of the static variable
     * @param value - the value to assign
     * @return the append command string
     */
    fun apocSetStatic(name: String, value: String) = "call apoc.static.set('$name', '$value')"


    /**
     * Gets a static value
     *
     * @param name - the name of the static variable
     * @return the append command string
     */
    fun apocGetStatic(name: String) = "call apoc.static.get('$name')"


    /**
     * Gets static value as String, assigning to variable.
     * Correctly converts the returned value into String form before assigning to variable
     *
     * @param name - the name of the static variable to get a value from
     * @param variable - the name of the variable to assign once converted to string
     * @return the append command string
     */
    fun apocGetStaticAsString(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.toString(value) AS $variable"

    /**
     * Gets static value as JSon, assigning to variable.
     * Correctly converts the returned value into JSon object form before assigning to variable
     *
     * @param name - the name of the static variable to get a value from
     * @param variable - the name of the variable to assign once converted to JSon
     * @return the append command string
     */
    fun apocGetStaticAsJson(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.fromJsonMap(apoc.convert.toString(value)) as $variable"


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

        fun apocLoadJdbc(init: ApocLoadJdbcContext.() -> Unit) {
            val context = ApocLoadJdbcContext().apply(init)
            query.append(context.build())
        }

        fun apocPeriodicIterate(init: ApocPeriodicIterateContext.() -> Unit) {
            val context = ApocPeriodicIterateContext().apply(init)
            query.append(context.build())
        }
    }


    class LoadCsvContext(var fileUrl: String, var variable: String = "row") {

        var cypher: String = ""

        fun cypher(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.cypher = context.build()
        }

        fun build(): String = trimAndJoin(
                """LOAD CSV WITH HEADERS FROM "$fileUrl" AS row WITH $variable""",
                cypher
        )
    }


    class ApocLoadJsonContext {

        var url: String = ""
        var headers = mutableMapOf<String, String>()
        var payload: String? = null
        var path: String? = null
        var with: String = "WITH value"
        var cypher: String = ""

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
                "CALL apoc.load.jsonParams(${params()}) YIELD value $with",
                cypher
        )
    }


    class ApocLoadJdbcContext {

        var url: String = ""
        var select: String = "LOAD_JDBC_SELECT_NOT_ASSIGNED"
        var with: String = "WITH row"
        var cypher: String = ""

        fun cypher(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.cypher = context.build()
        }

        fun build(): String = trimAndJoin(
                """
                CALL apoc.load.jdbc($url,
                ${select.trimIndent().escapeDoubleQuotes().quoted().prependIndent("    ")}
                ) YIELD row $with""".trimMargin(),
                cypher
        )
    }


    class ApocPeriodicIterateContext {

        var batchSize: Int = 1000
        var parallel: Boolean = false
        var outer: String = "PERIODIC_OUTER_NOT_ASSIGNED"
        var inner: String = "PERIODIC_INNER_NOT_ASSIGNED"

        fun outer(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.outer = context.build().trimIndent()
        }

        fun inner(init: QueryContext.() -> Unit) {
            val context = QueryContext().apply(init)
            this.inner = context.build().trimIndent()
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

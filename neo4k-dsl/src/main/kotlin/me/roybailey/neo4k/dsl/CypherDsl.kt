package me.roybailey.neo4k.dsl

import java.io.PrintWriter
import java.io.Writer


// ------------------------------------------------------------
// DSL Syntax Sugar root functions
// ------------------------------------------------------------

typealias ScriptLibrary = MutableMap<String, MutableList<QueryStatement>>

fun String.toNeo4j(dollar: String = "__"): String = CypherDsl.toNeo4j(this, dollar)
fun String.escapeDoubleQuotes() = CypherDsl.escapeQuotes(this)
fun String.quoted(doubleQuotes: Boolean = true, quoted: Boolean = true) = CypherDsl.quoted(this, doubleQuotes, quoted)
fun String.doubleQuoted() = CypherDsl.quoted(this, true)
fun String.singleQuoted() = CypherDsl.quoted(this, false)
fun String.withLabels(vararg labels: String) = CypherDsl.withLabels(this, *labels)

object CypherDsl {

    private fun trimAndJoin(vararg args: String) = args.map { it.trimIndent() }.joinToString(separator = "\n") { it.trimIndent() }

    private fun parameters(minimum: Int, vararg args: String): String {
        val params = mutableListOf<String>()
        args.take(minimum).forEach { params += it }
        args.slice(minimum..args.size).forEach { if (!it.isNullOrBlank()) params += it }
        return params.joinToString(",")
    }

    // ------------------------------------------------------------
    // String & Cypher Fragments
    // ------------------------------------------------------------

    /**
     * Replaces dollar substitute with dollar
     * @param value the value to alter
     * @param dollar the substitute to replace (defaults to __ double underscore)
     */
    fun toNeo4j(value: String, dollar: String = "__"): String = value.replace(dollar, "$")

    /**
     * Escapes double quotes in a string
     * @param value the value to escape double
     */
    fun escapeQuotes(value: String, doubleQuotes: Boolean = true): String = when (doubleQuotes) {
        true -> value.replace("\"", "\\\"")
        else -> value.replace("'", "\\'")
    }

    /**
     * Quotes a value
     * @param value the value to be quoted
     * @param doubleQuotes use double quotes (default); otherwise use single quotes
     * @param quoted flag to indicate whether to apply quotes (defaults to true)
     */
    fun quoted(value: Any, doubleQuotes: Boolean = true, quoted: Boolean = true): String =
            when (quoted) {
                true -> when (doubleQuotes) {
                    true -> "\"$value\""
                    else -> "\'$value\'"
                }
                else -> "" + value
            }

    /**
     * Joins labels to a node name
     * @param node the name of the node variable
     * @param labels the array of labels to apply
     */
    fun withLabels(node: String, vararg labels: String): String =
            if (labels.isNotEmpty()) node + ":" + labels.joinToString(":") else node


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
    // fun apocGetStaticAsString(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.toString(value) AS $variable"

    /**
     * Gets static value as JSon, assigning to variable.
     * Correctly converts the returned value into JSon object form before assigning to variable
     *
     * @param name - the name of the static variable to get a value from
     * @param variable - the name of the variable to assign once converted to JSon
     * @return the append command string
     */
    // fun apocGetStaticAsJson(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.fromJsonMap(apoc.convert.toString(value)) as $variable\n"


    // ------------------------------------------------------------
    // Root DSL starter methods
    // ------------------------------------------------------------

    fun scriptLibrary(init: ScriptDefinitionContext.() -> Unit): ScriptLibrary {
        val context = ScriptDefinitionContext().apply(init)
        return context.build()
    }

    fun statement(description: String = "", init: QueryStatementContext.() -> Unit): QueryStatement {
        val context = QueryStatementContext().apply(init)
        return context.build(description)
    }

    fun cypher(init: QueryContext.() -> Unit): String {
        val context = QueryContext().apply(init)
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


    class JoinContext(val separator: String = ",", vararg val list: String) {

        fun build(): String = when {
            list.isNotEmpty() -> list.joinToString(separator)
            else -> ""
        }
    }


    /**
     * Captures "{ name=value, name=value }" strings
     */
    class ParameterContext {

        var params: MutableMap<String, Any> = LinkedHashMap()

        fun param(name: String, value: Any) {
            params.put(name, value)
        }

        fun param(more: Map<String, Any>) {
            params.putAll(more)
        }

        fun param(vararg more: Pair<String, Any>) {
            params.putAll(more)
        }

        fun build(): String = when {
            params.isNotEmpty() -> " { " + params.map {
                it.key + ":" + when {
                    it.value is Array<*> -> "[" + (it.value as Array<*>).joinToString(",") + "]"
                    else -> it.value
                }
            }.joinToString(", ") + " }"
            else -> ""
        }
    }


    /**
     * Captures construction of Cypher query strings
     */
    open class QueryContext {

        var query = StringBuffer()

        fun append(cypher: String): QueryContext {
            query.append(cypher)
            return this
        }

        operator fun Any.unaryPlus() {
            query.append(when (this) {
                is Unit -> ""
                is String -> this
                is Array<*> -> this.joinToString(",") { this.toString() }
                is Collection<*> -> this.joinToString(",") { this.toString() }
                else -> this.toString()
            })
        }

        fun CREATE(cypher: String = "", vararg labels: String = emptyArray(), init: QueryContext .() -> Unit = {}) {
            val context = QueryContext().apply(init)
            val nodeAndLabels = withLabels(cypher, *labels)
            val parameters = context.build()
            val create = "$nodeAndLabels$parameters".let {
                when {
                    it.trim().isNotEmpty() -> "($it)"
                    else -> it
                }
            }
            query.append("CREATE $create".trim().toNeo4j()).append("\n")
        }

        fun RETURN(cypher: String) = query.append("RETURN ${cypher.toNeo4j()}\n")

        fun MERGE(cypher: String, vararg labels: String = emptyArray(), init: ParameterContext .() -> Unit = {}) {
            val context = ParameterContext().apply(init)
            query.append("MERGE (${withLabels(cypher, *labels)}${context.build()})\n".toNeo4j())
        }

        fun WITH(cypher: String) = query.append("WITH ${cypher.toNeo4j()}\n")
        fun CALL(cypher: String) = query.append("CALL ${cypher.toNeo4j()}\n")

        fun param(init: ParameterContext .() -> Unit = {}) {
            val context = ParameterContext().apply(init)
            query.append(context.build().toNeo4j())
        }

        fun param(params: Map<String, Any>) {
            param {
                param(params)
            }
        }

        fun param(vararg params: Pair<String, Any>) {
            param {
                param(*params)
            }
        }

        fun relationship(node1: String, relationship: String, node2: String, init: ParameterContext .() -> Unit = {}) {
            val context = ParameterContext().apply(init)
            query.append("($node1)-[:$relationship${context.build()}]->($node2)".toNeo4j())
        }

        fun build(): String = query.toString()
        override fun toString(): String = build()
    }


    class TopLevelQueryContext : QueryContext() {

        /**
         * Gets static value as String, assigning to variable.
         * Correctly converts the returned value into String form before assigning to variable
         *
         * @param name - the name of the static variable to get a value from
         * @param variable - the name of the variable to assign once converted to string
         * @return the append command string
         */
        fun apocGetStaticAsString(name: String, variable: String = "VALUE", init: QueryContext.() -> Unit = {}): String {
            val context = QueryContext().apply(init)
            val cypher = "CALL apoc.static.get('$name') yield value WITH apoc.convert.toString(value) AS $variable"
            query.append(cypher).append("\n")
            return cypher
        }

        /**
         * Gets static value as JSon, assigning to variable.
         * Correctly converts the returned value into JSon object form before assigning to variable
         *
         * @param name - the name of the static variable to get a value from
         * @param variable - the name of the variable to assign once converted to JSon
         * @return the append command string
         */
        fun apocGetStaticAsJson(name: String, variable: String = "VALUE", init: QueryContext.() -> Unit = {}): String {
            val context = QueryContext().apply(init)
            val cypher = "CALL apoc.static.get('$name') yield value WITH apoc.convert.fromJsonMap(apoc.convert.toString(value)) as $variable"
            query.append(cypher).append("\n")
            return cypher
        }

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

}

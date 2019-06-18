package me.roybailey.neo4k.api

import java.lang.Boolean.parseBoolean
import java.lang.Integer.parseInt


typealias QueryParams = Map<String, Any>

fun QueryParams.getBoolean(key: String, defaultValue: Boolean = true): Boolean {
    return parseBoolean(getOrDefault(key, defaultValue.toString()).toString())
}

fun QueryParams.getInt(key: String, defaultValue: Int = 0): Int {
    return parseInt(getOrDefault(key, defaultValue.toString()).toString())
}

fun QueryParams.getString(key: String, defaultValue: String = ""): String {
    return getOrDefault(key, defaultValue).toString()
}

fun emptyQueryParams(): QueryParams = mapOf()


interface QueryStatement {

    val description: String
    val query: String
    val defaultParams: QueryParams

    // return string for query created with parameters
    fun toQueryString(params: QueryParams = defaultParams) = query

    // return string for documentation or script file concatenation
    fun toScriptString(params: QueryParams = defaultParams) = "\n" + description + "\n" + toQueryString(params)

    companion object {

        const val REGEX_PARAM = "\\\$\\w+"

        data class QueryStatementParsingOptions(
                val commentPrefixes: List<String> = listOf("//", "##", "--"),
                val parameterRegex: Regex = Regex(REGEX_PARAM)
        )

        fun createQuery(
                description: String,
                statement: String,
                defaultParams: QueryParams = emptyQueryParams()
        ) = SimpleQueryStatement(
                description = description,
                query = statement,
                defaultParams = defaultParams
        )

        fun createScript(vararg statement: String) = statement.map {
            SimpleQueryStatement(
                    description = "Simple Query",
                    query = it,
                    defaultParams = emptyQueryParams()
            )
        }.toList()


        /**
         * Breaks a script of multiple statements down using comment lines as delimiters
         */
        fun parseQueryScriptStatements(
                script: String,
                parsingOptions: QueryStatementParsingOptions = QueryStatementParsingOptions()
        ): List<QueryStatement> {

            if (script.isNullOrEmpty())
                return emptyList()
            val target = mutableListOf<QueryStatement>()
            var comment = ""
            var statement = ""
            var parameters = mutableMapOf<String, Any>()
            val addStatement: () -> Unit = {
                target.add(SimpleQueryStatement(
                        query = statement.trimIndent(),
                        description = comment.trimIndent(),
                        defaultParams = parameters))
                comment = ""
                statement = ""
            }

            script.split("\n")
                    .filter { it.trim().isNotEmpty() }
                    .forEach { line ->
                        var endOfQuery = false
                        var isQuery = true
                        when {
                            parsingOptions.commentPrefixes.firstOrNull { line.startsWith(it) } != null -> {
                                // this is a comment
                                endOfQuery = true
                                isQuery = false
                                comment += line + "\n"
                            }
                            // end of a query
                            line.endsWith(";") -> endOfQuery = true
                            // this is a query
                            else -> isQuery = true
                        }
                        if(isQuery) {
                            statement += line.replace(";", "") + "\n"
                            parsingOptions.parameterRegex.findAll(line).forEach { parameters[it.value.replace("$","")] = Unit }
                        }
                        if (endOfQuery && statement.isNotEmpty()) {
                            addStatement()
                        }
                    }
            addStatement()

            return target
        }


        fun parseQueryScriptStatements(
                scripts: Map<String, String>,
                parsingOptions: QueryStatementParsingOptions = QueryStatementParsingOptions()
        ): Map<String, List<QueryStatement>> {
            val statements = mutableMapOf<String, List<QueryStatement>>()
            scripts.forEach { (key, value) -> statements[key] = parseQueryScriptStatements(value) }
            return statements
        }
    }
}


data class SimpleQueryStatement(
        override val description: String,
        override val query: String,
        override val defaultParams: QueryParams = emptyQueryParams()
) : QueryStatement


typealias QueryData = Map<String, Any?>


interface QueryResult {

    fun row(): Int
    fun columns(): List<String>
    fun data(): QueryData
    fun value(idx: Int) = data().get(columns()[idx])
    fun value(name: String) = data().get(name)

}


data class SimpleQueryResult(val row: Int, val columns: List<String>, val data: QueryData) : QueryResult {

    override fun row(): Int = row

    override fun columns(): List<String> = columns

    override fun data(): QueryData = data

}


typealias QueryResultConsumer = (QueryResult) -> Unit


typealias QueryExecutor = (query: QueryStatement, params: QueryParams, mapper: QueryResultConsumer) -> Unit

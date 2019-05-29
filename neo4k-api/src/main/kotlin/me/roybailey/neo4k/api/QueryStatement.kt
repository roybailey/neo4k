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
    val statement: String
    val defaultParams: QueryParams

    // return string for query created with parameters
    fun toQueryString(params: QueryParams = defaultParams) = statement

    // return string for documentation or script file concatenation
    fun toScriptString(params: QueryParams = defaultParams)
            = "\n" + description + "\n" + toQueryString(params)

    companion object {

        fun loadStatements(filename: String): List<QueryStatement> {
            return AbstractQuery.getQueryScriptStatements(AbstractQuery::class.java.getResource(filename).readText())
        }
    }
}


data class SimpleQueryStatement(
        override val description: String,
        override val statement: String,
        override val defaultParams: QueryParams = emptyQueryParams()
) : QueryStatement {

    companion object {

        fun neo4jQuery(
                description: String,
                statement: String,
                defaultParams: QueryParams = emptyQueryParams()
        ) = SimpleQueryStatement(
                description = description,
                statement = statement,
                defaultParams = defaultParams
        )

        fun neo4jScript(vararg statement: String) = statement.map {
            SimpleQueryStatement(
                    description = "Simple Neo4j Script",
                    statement = it,
                    defaultParams = emptyQueryParams()
            )
        }.toList()
    }

    override fun toQueryString(params: QueryParams): String = statement
}


open class AbstractQuery {

    companion object {

        fun loadStatements(filename: String): List<QueryStatement> {
            return getQueryScriptStatements(AbstractQuery::class.java.getResource(filename).readText())
        }

        /**
         * Breaks a script of multiple statements down using comment lines as delimiters
         */
        fun getQueryScriptStatements(script: String): List<QueryStatement> {

            if (script.isNullOrEmpty())
                return emptyList()
            val target = mutableListOf<QueryStatement>()
            var comment = ""
            var statement = ""
            val addStatement: () -> Unit = {
                target.add(SimpleQueryStatement(
                        statement = statement.trimIndent(),
                        description = comment.trimIndent(),
                        defaultParams = emptyQueryParams()))
                comment = ""
                statement = ""
            }

            script.split("\n")
                    .filter { it.trim().isNotEmpty() }
                    .forEach { line ->
                        if (line.startsWith("//") || line.startsWith("--")) {
                            // this is a comment
                            if (statement.isNotEmpty()) {
                                addStatement()
                            }
                            comment += line
                            comment += "\n"
                        } else {
                            // this is a statement
                            statement += line
                            statement += "\n"
                        }
                    }
            addStatement()

            return target
        }
    }

}

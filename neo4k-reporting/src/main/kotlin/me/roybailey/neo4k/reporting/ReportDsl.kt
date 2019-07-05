package me.roybailey.neo4k.reporting

import me.roybailey.neo4k.api.QueryParams
import me.roybailey.neo4k.api.QueryStatement
import me.roybailey.neo4k.api.SimpleQueryStatement
import me.roybailey.neo4k.api.emptyQueryParams


// ------------------------------------------------------------
// DSL Syntax Sugar
// ------------------------------------------------------------

fun reportDefinition(init: ReportDefinitionContext.() -> Unit): ReportDefinition {
    val context = ReportDefinitionContext().apply(init)
    return context.build()
}


fun queryStatement(init: QueryStatementContext.() -> Unit): QueryStatement {
    val context = QueryStatementContext()
            .apply(init)
    return context.build("")
}


// ------------------------------------------------------------
// DSL Context objects work like builders
// ------------------------------------------------------------

class ReportDefinitionContext {

    var name: String? = null
    var description: String? = ""
    var query: QueryStatement? = null
    var columns: List<ReportColumn> = emptyList()

    fun query(init: QueryStatementContext.() -> Unit) {
        val context = QueryStatementContext().apply(init)
        this.query = context.build(description)
    }

    fun build(): ReportDefinition {
        return ReportDefinition(this.name!!, query!!, columns)
    }
}


class QueryStatementContext {

    var description: String? = null
    var statement: String? = null
    var defaultParams = emptyQueryParams()

    fun QueryStatementContext.neo4j(cypher: String, defaultParams: QueryParams = this.defaultParams) {
        statement = cypher
        this.defaultParams = defaultParams
    }

    fun QueryStatementContext.sql(sql: String) {
        statement = sql
        this.defaultParams = defaultParams
    }

    fun build(defaultDescription: String?): QueryStatement =
            SimpleQueryStatement(
                    if (description == null) defaultDescription!! else description!!,
                    statement!!,
                    defaultParams
            )
}


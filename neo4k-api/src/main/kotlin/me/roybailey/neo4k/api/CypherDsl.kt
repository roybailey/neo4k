package me.roybailey.neo4k.api


// ------------------------------------------------------------
// DSL Syntax Sugar
// ------------------------------------------------------------

fun cypher(init: CypherContext.() -> Unit): String {
    val context = CypherContext().apply(init)
    return context.build()
}


// ------------------------------------------------------------
// DSL Context objects work like builders
// ------------------------------------------------------------

class CypherContext {

    var match: CypherMatch? = null

    fun match(init: CypherMatchContext.() -> Unit) {
        val context = CypherMatchContext().apply(init)
        match = context.build()
    }

    fun build(): String = match?.toCypherString() ?: ""
}

data class CypherMatch(val cypher: String) {
    fun toCypherString(): String = "match $cypher"
}

class CypherMatchContext {

    var cypher: String = ""

    fun build(): CypherMatch = CypherMatch(cypher)

}


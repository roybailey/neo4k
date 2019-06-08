package me.roybailey.neo4k.api


object Neo4jCypher {

    fun toNeo4j(cypher:String, dollar:String = "__") = cypher.replace(dollar, "$")

    // warning!!! deletes all data
    fun deleteAllData() = "match (n) optional match (n)-[r]-() delete r,n"

    // warning!!! deletes all data for given label
    fun deleteAllData(label: String) = "match (n:$label) optional match (n)-[r]-() delete r,n"

    // set static data
    fun apocSetStatic(name: String, value: String) = "call apoc.static.set('$name', '$value')"

    // get static data
    fun apocGetStatic(name: String) = "call apoc.static.get('$name')"

    fun getLabelCount() = """
            MATCH (a) WITH DISTINCT LABELS(a) AS temp, COUNT(a) AS tempCnt
            UNWIND temp AS label
            RETURN label, SUM(tempCnt) AS total
            ORDER BY label
            """.trimIndent()
}
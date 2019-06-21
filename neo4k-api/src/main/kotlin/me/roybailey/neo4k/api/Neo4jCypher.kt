package me.roybailey.neo4k.api


object Neo4jCypher {

    fun toNeo4j(cypher: String, dollar: String = "__") = cypher.replace(dollar, "$")

    // warning!!! deletes all data
    fun deleteAllData() = "match (n) optional match (n)-[r]-() delete r,n"

    // warning!!! deletes all data for given label
    fun deleteAllData(label: String) = "match (n:$label) optional match (n)-[r]-() delete r,n"

    fun getLabelCount() = """
            MATCH (a) WITH DISTINCT LABELS(a) AS temp, COUNT(a) AS tempCnt
            UNWIND temp AS label
            RETURN label, SUM(tempCnt) AS total
            ORDER BY label
            """.trimIndent()

    fun loadCsvWithHeaders(fileUrl: String, withLineCypher: String) = """
        LOAD CSV WITH HEADERS FROM "$fileUrl" AS row WITH row
        $withLineCypher
    """.trimIndent()
}
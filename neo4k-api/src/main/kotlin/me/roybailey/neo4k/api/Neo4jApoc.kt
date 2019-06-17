package me.roybailey.neo4k.api

import mu.KotlinLogging


class Neo4jApoc(val neo4j: Neo4jService) {

    companion object {

        // set static data
        fun apocSetStatic(name: String, value: String) = "call apoc.static.set('$name', '$value')"

        // get static data
        fun apocGetStatic(name: String) = "call apoc.static.get('$name')"

        // load JDBC
        fun apocLoadJdbc(
                apocStaticUrl: String,
                sql: String,
                merge: String,
                batchsize: Int = 1000,
                parallel: Boolean = false
        ) = """
            CALL apoc.periodic.iterate("
              CALL apoc.static.get('$apocStaticUrl') yield value WITH apoc.convert.toString(value) AS DB_URL
              CALL apoc.load.jdbc(DB_URL,\"$sql\") YIELD row RETURN row
            ","$merge", {batchSize:$batchsize, parallel:$parallel})
        """.trimIndent()
    }


    private val LOG = KotlinLogging.logger {}


    fun setStatic(key: String, value: Any): Neo4jApoc {
        // set static global variables such as sensitive connection values...
        neo4j.execute(apocSetStatic(key, value.toString()), emptyMap()) {
            LOG.info { it.next() }
        }
        return this
    }


    fun getStatic(key: String, defaultValue: Any? = null): Any? {
        var result: Any? = null
        neo4j.execute(apocGetStatic(key), emptyMap()) {
            if (it.hasNext())
                result = it.single()["value"]
        }
        return result?.let { result } ?: defaultValue.also {
            LOG.warn { "Apoc Static value Not Found for $key, using defaultValue=$defaultValue" }
        }
    }

}

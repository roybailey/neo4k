package me.roybailey.neo4k.api

import mu.KotlinLogging


class Neo4jApoc(val neo4j: Neo4jService) {

    companion object {

        // set static data
        fun apocSetStatic(name: String, value: String) = "call apoc.static.set('$name', '$value')"

        // get static data
        fun apocGetStatic(name: String) = "call apoc.static.get('$name')"


        // prepare a cypher variable value, either directly or from static named variable
        fun getVariable(
                name: String,
                value: String,
                fromStatic: Boolean = false,
                singleQuote: Boolean = false) = when (fromStatic) {
            true -> "CALL apoc.static.get('$value') yield value WITH apoc.convert.toString(value) AS $name"
            else -> "WITH '$value' AS $name"
        }


        /**
         * Apoc json load cypher command, e.g.
         *
        WITH "https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=creation&tagged=neo4j&site=stackoverflow&filter=!5-i6Zw8Y)4W7vpy91PMYsKM-k9yzEsSC1_Uxlf" AS url
        CALL apoc.load.json(url) YIELD value
        UNWIND value.items AS item
        RETURN item.title, item.owner, item.creation_date, keys(item)
         */
        fun apocLoadJson(url: String, process: String) = """
            WITH "$url" as url
            CALL apoc.load.json(url) YIELD value
            $process
        """.trimIndent()


        /**
         * Apoc jdbc load cypher command, e.g.
         *
        CALL apoc.load.jdbc('jdbc:h2:mem:test;DB_CLOSE_DELAY=-1',"SELECT * FROM CSVREAD('sample.csv')") YIELD row
        WITH row
        RETURN
        row.PRODUCT as PRODUCT,
        row.FULLNAME as FULLNAME,
        row.PRICE as PRICE,
        row.UNITPRICE as UNITPRICE,
        apoc.text.toUpperCase(COALESCE(row.CATEGORY, "")) as CATEGORY,
        row.BRAND as BRAND,
        row.QUANTITY as QUANTITY,
        row.DISCOUNT as DISCOUNT
         */
        fun apocLoadJdbc(dbUrl: String, sql: String, process: String) = """
            WITH "$dbUrl" AS dbUrl
            CALL apoc.load.jdbc(dbUrl,"$sql") YIELD row
            WITH row
            $process
        """.trimIndent()


        /**
         * Apoc jdbc batch load cypher command wrapped in periodic commit...
         */
        fun apocLoadJdbcBatch(
                dbUrl: String,
                sql: String,
                process: String,
                batchsize: Int = 1000,
                parallel: Boolean = false,
                useStaticDbUrl: Boolean = true
        ) = """
            CALL apoc.periodic.iterate("
              ${getVariable(name = "DB_URL", value = dbUrl, fromStatic = useStaticDbUrl, singleQuote = true)}
              CALL apoc.load.jdbc(DB_URL,\"$sql\") YIELD row RETURN row
            ","$process", {batchSize:$batchsize, parallel:$parallel})
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

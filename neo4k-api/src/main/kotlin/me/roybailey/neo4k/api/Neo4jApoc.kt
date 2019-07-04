package me.roybailey.neo4k.api

import mu.KotlinLogging


class Neo4jApoc(val neo4j: Neo4jService) {

    companion object {


        /**
         * Sets a static value
         *
         * @param name - the name of the static variable
         * @param value - the value to assign
         * @return the cypher command string
         */
        fun apocSetStatic(name: String, value: String) = "call apoc.static.set('$name', '$value')"


        /**
         * Gets a static value
         *
         * @param name - the name of the static variable
         * @return the cypher command string
         */
        fun apocGetStatic(name: String) = "call apoc.static.get('$name')"


        /**
         * Gets static value as String, assigning to variable.
         * Correctly converts the returned value into String form before assigning to variable
         *
         * @param name - the name of the static variable to get a value from
         * @param variable - the name of the variable to assign once converted to string
         * @return the cypher command string
         */
        fun apocGetStaticAsString(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.toString(value) AS $variable"

        /**
         * Gets static value as JSon, assigning to variable.
         * Correctly converts the returned value into JSon object form before assigning to variable
         *
         * @param name - the name of the static variable to get a value from
         * @param variable - the name of the variable to assign once converted to JSon
         * @return the cypher command string
         */
        fun apocGetStaticAsJson(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.fromJsonMap(apoc.convert.toString(value)) AS $variable"


        /**
         * Prepare a cypher variable nameOrValue, either directly or from static named variable
         *
         * @param nameOrValue - the variable of the static variable to get a nameOrValue from; otherwise the
         * @param variable - the variable to assign the nameOrValue to
         * @param fromStatic - boolean to route from static value or direct value
         * @return the cypher command string
         */
        fun getVariable(
                nameOrValue: String,
                variable: String = "VALUE",
                fromStatic: Boolean = false,
                singleQuote: Boolean = false) = when (fromStatic) {
            true -> apocGetStaticAsString(nameOrValue, variable)
            else -> "WITH '$nameOrValue' AS $variable"
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
         * Apoc jdbc batch load cypher command wrapped in periodic commit.
         * Tricky to construct because it is a function taking two strings, one for the SQL and one for the cypher MERGE,
         * but as these strings can contain nested levels of quotes you need to correctly escape or switch between single
         * and double quotes.
         * Here's an example of full cypher for loading in batch from SQL statement...
         *
        CALL apoc.periodic.iterate("
        WITH 'jdbc:h2:mem:test;DB_CLOSE_DELAY=-1' AS DB_URL
        CALL apoc.load.jdbc(DB_URL,\"SELECT * FROM CSVREAD('sample.csv')\") YIELD row RETURN row
        ","
        MERGE (c:Country {country: apoc.text.toUpperCase(COALESCE(row.COUNTRY, 'unknown'))})
        SET c.region = row.REGION
        MERGE (p:Product {product: row.`Item Type`})
        MERGE (o:Order {orderId: row.`Order ID`})
        SET o.salesChannel = row.`Sales Channel`,
        o.orderPriority = row.`Order Priority`,
        o.orderDate = row.`Order Date`,
        o.shipDate = row.`Ship Date`,
        o.quantity = row.`Units Sold`,
        o.unitPrice = row.`Unit Price`
        MERGE (o)-[:FROM]->(c)
        MERGE (o)-[:OF]->(p)
        RETURN count(p) as totalProducts
        ", {batchSize:100, parallel:false})
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
              ${getVariable(variable = "DB_URL", nameOrValue = dbUrl, fromStatic = useStaticDbUrl, singleQuote = true)}
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

package me.roybailey.neo4k.api

import mu.KotlinLogging


class Neo4jApoc(val neo4j: Neo4jService) {

    companion object {


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
        fun apocGetStaticAsString(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.toString(value) AS $variable"

        /**
         * Gets static value as JSon, assigning to variable.
         * Correctly converts the returned value into JSon object form before assigning to variable
         *
         * @param name - the name of the static variable to get a value from
         * @param variable - the name of the variable to assign once converted to JSon
         * @return the append command string
         */
        fun apocGetStaticAsJson(name: String, variable: String = "VALUE") = "CALL apoc.static.get('$name') yield value WITH apoc.convert.fromJsonMap(apoc.convert.toString(value)) as $variable"

    }


    private val logger = KotlinLogging.logger {}


    fun setStatic(key: String, value: Any): Neo4jApoc {
        // set static global variables such as sensitive connection values...
        neo4j.execute(apocSetStatic(key, value.toString()), emptyMap()) {
            logger.info { it.next() }
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
            logger.warn { "Apoc Static value Not Found for $key, using defaultValue=$defaultValue" }
        }
    }

}

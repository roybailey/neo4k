package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.Neo4jServiceRecord
import org.neo4j.driver.v1.Record

class Neo4jBoltRecord(val record: Record) : Neo4jServiceRecord {
    override fun keys(): List<String> = record.keys()
    override fun values(): List<Any> = record.values().map { it.asObject() }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = record.index(lookup)
    override fun get(key: String): Any = record.get(key).asObject()
    override fun get(index: Int): Any = record.get(index).asObject()
    override fun size(): Int = record.size()
    override fun asMap(): Map<String, Any> = record.asMap()
    override fun fields(): List<Pair<String, Any>> = record.fields().map { Pair(it.key(), it.value().asObject()) }
}

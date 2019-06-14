package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.Neo4jMapRecord
import me.roybailey.neo4k.api.Neo4jServiceRecord
import org.neo4j.driver.internal.value.NodeValue
import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.Value
import org.neo4j.driver.v1.types.Node

class Neo4jBoltRecord(val record: Record) : Neo4jServiceRecord {

    companion object {
        fun unwrapValue(data: Value) = when (data.asObject()) {
            is Node -> Neo4jMapRecord(data.asNode().asMap(), "id" to data.asNode().id())
            else -> data.asObject() as Any
        }
    }

    override fun keys(): List<String> = record.keys()
    override fun values(): List<Any> = record.values().map { unwrapValue(it) }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = record.index(lookup)
    override fun get(key: String): Any = unwrapValue(record.get(key))
    override fun get(index: Int): Any = unwrapValue(record.get(index))
    override fun size(): Int = record.size()
    override fun asMap(): Map<String, Any> = record.asMap()
    override fun fields(): List<Pair<String, Any>> = record.fields().map { Pair(it.key(), unwrapValue(it.value())) }
}

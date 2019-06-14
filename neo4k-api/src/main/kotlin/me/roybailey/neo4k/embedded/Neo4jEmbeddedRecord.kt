package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.Neo4jServiceRecord
import org.neo4j.kernel.impl.core.NodeProxy

class Neo4jEmbeddedRecord(val record: Map<String, Any>) : Neo4jServiceRecord {

    companion object {
        fun unwrapValue(data: Any) = when (data) {
            is NodeProxy -> Neo4jEmbeddedRecord(data.allProperties.also { it["id"] = data.id })
            else -> data
        }
    }

    override fun keys(): List<String> = record.entries.map { it.key }
    override fun values(): List<Any> = record.entries.map { unwrapValue(it.value) }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = keys().indexOf(lookup)
    override fun get(key: String): Any = unwrapValue(record.getOrDefault(key, Unit))
    override fun get(index: Int): Any = unwrapValue(fields()[index].second)
    override fun size(): Int = record.size
    override fun asMap(): Map<String, Any> = record
    override fun fields(): List<Pair<String, Any>> = record.entries.map { Pair(it.key, unwrapValue(it.value)) }
}

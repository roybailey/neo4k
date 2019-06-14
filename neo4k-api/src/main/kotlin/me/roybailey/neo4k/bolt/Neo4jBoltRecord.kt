package me.roybailey.neo4k.bolt

import me.roybailey.neo4k.api.Neo4jMapRecord
import me.roybailey.neo4k.api.Neo4jServiceRecord
import org.neo4j.driver.internal.value.NodeValue
import org.neo4j.driver.v1.Record
import org.neo4j.driver.v1.Value
import org.neo4j.driver.v1.Values
import org.neo4j.driver.v1.types.Entity
import org.neo4j.driver.v1.types.Node
import org.neo4j.driver.v1.types.Path
import org.neo4j.driver.v1.types.Relationship
import org.neo4j.driver.v1.util.Function

class Neo4jBoltRecord(val record: Record) : Neo4jServiceRecord {

    override fun keys(): List<String> = record.keys()
    override fun values(): List<Any> = record.values().map { unwrapBoltValue(it) }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = record.index(lookup)
    override fun get(key: String): Any = unwrapBoltValue(record.get(key))
    override fun get(index: Int): Any = unwrapBoltValue(record.get(index))
    override fun size(): Int = record.size()
    override fun asMap(): Map<String, Any> = record.asMap()
    override fun fields(): List<Pair<String, Any>> = record.fields().map { Pair(it.key(), unwrapBoltValue(it.value())) }


    companion object {

        fun createBoltPair(key: String, value: Any) =
                object : org.neo4j.driver.v1.util.Pair<String, Value> {
                    override fun key(): String = key
                    override fun value(): Value = createBoltValue(value)
                }

        fun createBoltValue(data: Any): Value =
                when (data) {
                    is Neo4jServiceRecord -> NodeValue(object : Node {
                        override fun values(): MutableIterable<Value> = data.values().map { createBoltValue(it) }.toMutableList()
                        override fun <T : Any?> values(p0: Function<Value, T>?): MutableIterable<T> = TODO("not implemented")
                        override fun id(): Long = if (data.containsKey("id")) data.get("id") as Long else 0L
                        override fun hasLabel(p0: String?): Boolean = TODO("not implemented")
                        override fun size(): Int = data.size()
                        override fun asMap(): MutableMap<String, Any> = mutableMapOf<String, Any>().also { it.putAll(data.asMap()) }
                        override fun <T : Any?> asMap(p0: Function<Value, T>?): MutableMap<String, T> = TODO("not implemented")
                        override fun containsKey(key: String?): Boolean = data.containsKey(key!!)
                        override fun get(key: String?): Value = createBoltValue(data.get(key!!))
                        override fun labels(): MutableIterable<String> = TODO("not implemented")
                        override fun keys(): MutableIterable<String> = data.keys().toMutableList()
                    })
                    else -> Values.value(data)
                }

        fun unwrapBoltValue(data: Value) = when (data.asObject()) {
            is Node -> Neo4jMapRecord(data.asNode().asMap(), "id" to data.asNode().id())
            else -> data.asObject() as Any
        }

        fun createBoltRecord(record: Neo4jServiceRecord): Record =
            object : Record {
                override fun values(): MutableList<Value> = record.values().map { createBoltValue(it) }.toMutableList()

                override fun index(key: String?): Int = record.index(key!!)

                override fun size(): Int = record.size()

                override fun asMap(): MutableMap<String, Any> = record.asMap().toMutableMap()

                override fun <T : Any?> asMap(code: Function<Value, T>?): MutableMap<String, T> = TODO("not implemented")

                override fun get(key: String?): Value = record[key!!].run {
                    when (this) {
                        is Neo4jServiceRecord -> createBoltValue(this)
                        else -> Values.value(this)
                    }
                }

                override fun get(index: Int): Value = record[index].run {
                    when (this) {
                        is Neo4jServiceRecord -> createBoltValue(this)
                        else -> Values.value(this)
                    }
                }

                override fun get(key: String?, defaultValue: Value?): Value = TODO("not implemented")
                override fun get(key: String?, defaultValue: Any?): Any = TODO("not implemented")
                override fun get(key: String?, defaultValue: Number?): Number  = TODO("not implemented")
                override fun get(key: String?, defaultValue: Entity?): Entity  = TODO("not implemented")
                override fun get(key: String?, defaultValue: Node?): Node  = TODO("not implemented")
                override fun get(key: String?, defaultValue: Path?): Path  = TODO("not implemented")
                override fun get(key: String?, defaultValue: Relationship?): Relationship  = TODO("not implemented")
                override fun get(key: String?, defaultValue: MutableList<Any>?): MutableList<Any>  = TODO("not implemented")
                override fun <T : Any?> get(key: String?, defaultValue: MutableList<T>?, code: Function<Value, T>?): MutableList<T>
                        = TODO("not implemented")
                override fun get(key: String?, defaultValue: MutableMap<String, Any>?): MutableMap<String, Any>  = TODO("not implemented")
                override fun <T : Any?> get(key: String?, defaultValue: MutableMap<String, T>?, code: Function<Value, T>?): MutableMap<String, T>
                        = TODO("not implemented")
                override fun get(key: String?, defaultValue: Int): Int = TODO("not implemented")
                override fun get(key: String?, defaultValue: Long): Long = TODO("not implemented")
                override fun get(key: String?, defaultValue: Boolean): Boolean = TODO("not implemented")
                override fun get(key: String?, defaultValue: String?): String = TODO("not implemented")
                override fun get(key: String?, defaultValue: Float): Float = TODO("not implemented")
                override fun get(key: String?, defaultValue: Double): Double = TODO("not implemented")

                override fun containsKey(key: String?): Boolean = key?.let { record.containsKey(key) } ?: false
                override fun fields(): MutableList<org.neo4j.driver.v1.util.Pair<String, Value>> = record.fields().map { createBoltPair(it.first, it.second) }.toMutableList()
                override fun keys(): MutableList<String> = record.keys().toMutableList()
            }

    }
}

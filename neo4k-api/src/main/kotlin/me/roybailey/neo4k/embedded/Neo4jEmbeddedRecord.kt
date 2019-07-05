package me.roybailey.neo4k.embedded

import me.roybailey.neo4k.api.Neo4jServiceRecord
import org.neo4j.graphdb.*

class Neo4jEmbeddedRecord(val record: Map<String, Any>) : Neo4jServiceRecord {

    override fun keys(): List<String> = record.entries.map { it.key }
    override fun values(): List<Any> = record.entries.map { unwrapEmbeddedValue(it.value) }
    override fun containsKey(lookup: String): Boolean = record.containsKey(lookup)
    override fun index(lookup: String): Int = keys().indexOf(lookup)
    override fun get(key: String): Any? = record[key]?.let { unwrapEmbeddedValue(it) }
    override fun get(index: Int): Any? = unwrapEmbeddedValue(fields()[index].second)
    override fun size(): Int = record.size
    override fun asMap(): Map<String, Any> = record
    override fun fields(): List<Pair<String, Any>> = record.entries.map { Pair(it.key, unwrapEmbeddedValue(it.value)) }
    override fun toString(): String = super.toString() + keys()

    companion object {

        fun createEmbeddedValue(data: Any): Any =
                when (data) {
                    is Neo4jServiceRecord -> object : Node {

                        override fun hasProperty(key: String?): Boolean = key?.let { data.containsKey(key) } ?: false
                        override fun getLabels(): MutableIterable<Label> = TODO("not implemented")
                        override fun getAllProperties(): MutableMap<String, Any> = data.asMap().toMutableMap()
                        override fun addLabel(label: Label?) = TODO("not implemented")
                        override fun getGraphDatabase(): GraphDatabaseService = TODO("not implemented")
                        override fun setProperty(key: String?, value: Any?) = TODO("not implemented")
                        override fun getId(): Long = TODO("not implemented")
                        override fun hasLabel(label: Label?): Boolean = TODO("not implemented")
                        override fun getDegree(): Int = TODO("not implemented")
                        override fun getDegree(reltype: RelationshipType?): Int = TODO("not implemented")
                        override fun getDegree(direction: Direction?): Int = TODO("not implemented")
                        override fun getDegree(reltype: RelationshipType?, direction: Direction?): Int = TODO("not implemented")
                        override fun getRelationships(): MutableIterable<Relationship> = TODO("not implemented")
                        override fun getRelationships(vararg reltypes: RelationshipType?): MutableIterable<Relationship> = TODO("not implemented")
                        override fun getRelationships(direction: Direction?, vararg reltypes: RelationshipType?): MutableIterable<Relationship> = TODO("not implemented")
                        override fun getRelationships(direction: Direction?): MutableIterable<Relationship> = TODO("not implemented")
                        override fun getRelationships(reltype: RelationshipType?, direction: Direction?): MutableIterable<Relationship> = TODO("not implemented")
                        override fun removeLabel(label: Label?) = TODO("not implemented")
                        override fun removeProperty(key: String?): Any = TODO("not implemented")
                        override fun getProperties(vararg props: String?): MutableMap<String, Any> = TODO("not implemented")
                        override fun getProperty(key: String?): Any? = data[key!!]?.let { createEmbeddedValue(it) }
                        override fun getProperty(key: String?, defaultValue: Any?): Any = TODO("not implemented")
                        override fun getSingleRelationship(reltype: RelationshipType?, direction: Direction?): Relationship = TODO("not implemented")
                        override fun getRelationshipTypes(): MutableIterable<RelationshipType> = TODO("not implemented")
                        override fun createRelationshipTo(node: Node?, reltype: RelationshipType?): Relationship = TODO("not implemented")
                        override fun getPropertyKeys(): MutableIterable<String> = data.keys().toMutableList()
                        override fun hasRelationship(): Boolean = TODO("not implemented")
                        override fun hasRelationship(vararg reltypes: RelationshipType?): Boolean = TODO("not implemented")
                        override fun hasRelationship(direction: Direction?, vararg reltypes: RelationshipType?): Boolean = TODO("not implemented")
                        override fun hasRelationship(direction: Direction?): Boolean = TODO("not implemented")
                        override fun hasRelationship(reltype: RelationshipType?, direction: Direction?): Boolean = TODO("not implemented")
                        override fun delete() = TODO("not implemented")
                    }
                    else -> data
                }

        fun unwrapEmbeddedValue(data: Any) = when (data) {
            is Node -> Neo4jEmbeddedRecord(data.allProperties.also { it["id"] = data.id })
            else -> data
        }

        fun createEmbeddedRecord(record: Neo4jServiceRecord): Map<String, Any> = record.asMap()
    }
}

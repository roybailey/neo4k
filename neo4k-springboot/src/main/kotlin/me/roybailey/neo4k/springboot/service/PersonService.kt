package me.roybailey.neo4k.springboot.service

import me.roybailey.neo4k.api.Neo4jService
import me.roybailey.neo4k.api.Neo4jServiceRecord
import me.roybailey.neo4k.api.toNeo4j
import me.roybailey.neo4k.springboot.storage.Movie
import me.roybailey.neo4k.springboot.storage.Person
import org.springframework.stereotype.Component


@Component
class PersonService(val neo4jService: Neo4jService) {


    fun people(): List<Person> {
        val people = mutableListOf<Person>()
        neo4jService.execute("match (p:Person) return p limit 100") { result ->
            while (result.hasNext()) {
                val record = result.next()["p"] as Neo4jServiceRecord
                people.add(Person(
                        id = record["id"] as Long,
                        name = record["name"] as String,
                        born = (record["born"] as Long).toInt()
                ))
            }
        }
        return people.toList()
    }

}
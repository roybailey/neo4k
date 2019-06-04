# neo4k

Neo4j wrapper for Kotlin

## Installation

##### Maven 

```
    <dependency>
        <groupId>me.roybailey</groupId>
        <artifactId>neo4k-api</artifactId>
        <version>0.1.0</version>
    </dependency>
```

##### Gradle 

`implementation("me.roybailey:neo4k-api:0.1.0")`

## Getting Started

Key Concept | Description
------------|------------
`Neo4jService` | the low-level neo4j driver interface, through which all cypher is executed

#### Creating an embedded instance of a Neo4j Service

Create an embedded instance using the `file://` uri.  

```
    // load propoerties for neo4j service
    val options = Neo4jServiceOptions(
                        neo4jUri = "file://./target/neo4j/Neo4jServiceEmbeddedTest-{timestamp}",
                        boltPort = 7987
                    )
    vak neo4jService = Neo4jService.getInstance(options)
```

The embedded service reads neo4j specific properties from `/neo4j.conf` on the classpath.
Below are some examples of what might be in this config file.  See Neo4j documentation for complete list.

```
# these properties allow us to control the transaction log handling, reduce disk space usage
dbms.tx_log.rotation.retention_policy=1 files
dbms.checkpoint.interval.time=1m
dbms.memory.heap.initial_size=2000

# these properties are not used by Neo4j but are custom properties to activate bolt connector to embedded database
# when active it means you can access your embedded database from a local Noe4j Browser session
# neo4j.bolt.connector.port=7887

apoc.jdbc.TESTH2.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1

apoc.import.file.enabled=true
```

#### Creating a remote bolt instance of Neo4j Service

Create a remote bolt instance using the `bolt://` uri

```
    // load propoerties for neo4j service
    val options = Neo4jServiceOptions(
                        neo4jUri = "bolt://localhost",
                        boltPort = 7987
                    )
    vak neo4jService = Neo4jService.getInstance(options)
```

#### Options for configuring Neo4j Service driver

option | description
-------|------------
`neo4jUri` | the neo4j connection uri (either starting `file:` for embedded or `bolt:` for remote connection
`boltPort` | port to connect to bolt or to expose embedded to bolt (for viewing embedded via Neo4j browser)
`username` | neo4j username (required for bolt connections)
`password` | neo4j password (required for bolt connections)
`neo4jProcedures` | neo4j procedures to register (embedded) or verify (bolt)
`ignoreErrorOnDrop` | ignores errors from cypher commands that try to drop indexes that don't exist
`ignoreProcedureNotFound` | ignores failed neo4j procedure registrations or verifications

#### Connecting to the embedded instance from Neo4j Browser

1. make sure you are running in embedded mode and assign a unique port e.g. `boltPort=7987`
1. run command `:server disconnect` in the neo4j browser
1. enter `bolt://<machine>:<port>` in the connection URL field e.g. `bolt://localhost:7987`
1. leave `username` and `password` blank

This should get you connected to your running embedded database and allow you to query it using the browser. 




# neo4k

Neo4j wrapper for Kotlin


## Motivation and Design Guide

* Create a Kotlin friendly wrapper for accessing Neo4j
* Support Neo4j Embedded and Bolt drivers directly
* Same neo4j interface working with Embedded or Bolt drivers
* No other frameworks (no Spring or neo4j-ogm)
* Simple mapping to clean kotlin data classes (MyBatis like inspiration)

#### Primary Use-Cases Considered

* Simple way to add neo4j embedded capabilities into a micro-service
* Neo4j graph hydration, query, drop (simple small analytics) 

#### Use-Cases Definitely NOT Considered

* Large scale or high performance data graph projects


## User Guide

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


## Developers Guide

You will need to install an instance of Neo4j and run a graph database instance configured on `7987` for the tests to work.
This is so all the capabilities can be tested against both embedded and bolt driver versions.

* An instance of Neo4j Graph Database (check for compatible version used in project)
* The Neo4j Graph Database to be available on port `7987`  
* The h2 database jar installed in the plugins folder  

> Warning : Database will be cleared with every test, hence the project does not use the default port

#### Installing h2 database plugin

Find the neo4j database instance plugins folder e.g.

`cp ~/.m2/repository/com/h2database/h2/1.4.196/h2-1.4.196.jar <neo4j-installation-folder>/plugins` 

If you're using an instance of Neo4j Desktop, goto your graph project and select 'manage' from the graph instance.
At the top there is an 'Open Folder' button, which will take you to the installation folder and you should find 'plugins'
folder under there.

Note : if you get an error after adding the h2 jar to the plugins folder, check the neo4j.log file for errors.
Could be you need an older version of h2 to run on the Neo4j JVM version. 

#### Building the project

* `mvn clean install`






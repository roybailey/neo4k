#!/bin/zsh
# Capture dependency:tree changes (strip out the noise and timestamps)
mvn dependency:tree | awk '/Reactor Build Order/,/Reactor Summary for Neo4k/' > mvndt.txt

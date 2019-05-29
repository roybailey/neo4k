package me.roybailey.neo4k.api

import mu.KotlinLogging


abstract class BaseTest {

    val LOG = KotlinLogging.logger(this.javaClass.name)

}
package me.roybailey.neo4k.springboot.config

import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component


@Component
open class ApplicationBootstrap(
        val graphBootstrap: GraphBootstrap
) : ApplicationListener<ApplicationReadyEvent> {

    private val LOG = KotlinLogging.logger {}

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        LOG.info { "onApplicationEvent($event)" }
        graphBootstrap.initializeGraph()
    }
}

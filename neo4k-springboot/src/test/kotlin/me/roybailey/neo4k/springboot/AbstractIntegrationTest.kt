package me.roybailey.neo4k.springboot

import mu.KotlinLogging
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles(value = ["test"])
@RunWith(SpringRunner::class)
@SpringBootTest
abstract class AbstractIntegrationTest {

    val LOG = KotlinLogging.logger {}

}
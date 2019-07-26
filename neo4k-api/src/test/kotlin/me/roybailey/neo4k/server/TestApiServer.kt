package me.roybailey.neo4k.server

import io.javalin.Javalin
import org.testcontainers.Testcontainers
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap


class TestApiServer(val name: String, val port: Int) {

    companion object {

        fun findFreePort(): Int {
            val serverSocket = ServerSocket(0)
            val port = serverSocket.localPort
            serverSocket.close()
            return port
        }

        private val hashServers = ConcurrentHashMap<String, TestApiServer>()

        fun createTestApiServer(name: String = "default", port: Int = findFreePort()): TestApiServer {
            if (!hashServers.containsKey(name))
                hashServers[name] = TestApiServer(name, port)
            return testApiServer(name)
        }

        fun testApiServer(name: String = "default"): TestApiServer {
            return hashServers[name]!!
        }

        fun getTestApiServers(): List<TestApiServer> = hashServers.values.toList()
    }

    lateinit var app: Javalin

    fun start(data: Any) {
        app = Javalin.create().start(port)
        app.get("/testdata") { ctx ->
            ctx.json(data)
        }
    }

    fun stop() {
        app.stop()
    }

    val url: String = "http://localhost:$port"

}
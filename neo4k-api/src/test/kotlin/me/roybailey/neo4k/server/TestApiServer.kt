package me.roybailey.neo4k.server

import io.javalin.Javalin
import java.net.ServerSocket


class TestApiServer {

    companion object {
        fun findFreePort(): Int {
            val serverSocket = ServerSocket(0)
            val port = serverSocket.localPort
            serverSocket.close()
            return port
        }
    }

    lateinit var app:Javalin
    val port = findFreePort()

    fun start(data:Any) {
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
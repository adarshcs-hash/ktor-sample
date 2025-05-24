package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.DatabaseConfig

fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port) {
        connectToDatabase()
        module()
    }.start(wait = true)
}

fun Application.module() {
   // configureSecurity()
    configureSerialization()
    configureRouting()
    configureAuth()
}

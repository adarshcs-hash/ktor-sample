package com.example

import SupabaseClient
import io.ktor.server.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port) {
module()
    }.start(wait = true)


}

fun Application.module() {
    val httpClient = HttpClient(CIO)


    //configureShutdown(httpClient)
   // configureSecurity()
    DatabaseFactory.init()
    configureSerialization()
    configureRouting(SupabaseClient, httpClient)
    configureAuth()
}

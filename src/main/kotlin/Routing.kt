package com.example
import SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.Users
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

import kotlinx.coroutines.runBlocking

//val supabase = SupabaseClient("https://your-supabase-project-url.supabase.co", "your-supabase-anon-key")

fun Application.configureRouting(SupabaseClient: SupabaseClient, httpClient: HttpClient) {
    routing {
        get("/") {
            val htmlContent = runBlocking {
                val storage = SupabaseClient.instance.storage
                val response = storage.from("html-files").publicUrl("Index.html")
                val html = kotlin.runCatching {
                    httpClient.get(response).bodyAsText()
                }.getOrElse {
                    "<!DOCTYPE html><html><body><h1>Default Content</h1></body></html>"
                }
                html
            }
            call.respondText(htmlContent, contentType = ContentType.Text.Html)
        }
        // Static plugin. Try to access `/static/index.html`
       // staticResources("/static", "static")

        get("/test-db") {
            val count = transaction { Users.selectAll().count() }
            call.respondText("Users count: $count")
        }
    }
}

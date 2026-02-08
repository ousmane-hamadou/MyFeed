package com.github.ousmane_hamadou.server

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty) {
        routing {
            get("/") {
                call.respondText("Hello, world!")
            }
        }
    }.start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
            call.respondText(text = "Hello, world!", contentType = ContentType.Text.Plain)
        }
    }
}
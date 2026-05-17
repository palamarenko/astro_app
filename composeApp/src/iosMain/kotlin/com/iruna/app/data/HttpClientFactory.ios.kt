package com.iruna.app.data

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

@Suppress("UNCHECKED_CAST")
actual fun createHttpClient(
    json: Json,
    extraConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient {
    val typedConfig = extraConfig as HttpClientConfig<DarwinClientEngineConfig>.() -> Unit
    return HttpClient(Darwin) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) { println("Ktor: $message") }
            }
            level = LogLevel.ALL
        }
        typedConfig()
    }
}

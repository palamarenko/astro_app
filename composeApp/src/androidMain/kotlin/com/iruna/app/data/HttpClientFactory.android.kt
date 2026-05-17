package com.iruna.app.data

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** TrustManager, который принимает любой сертификат (только для дев-сборок/эмулятора). */
private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
})

actual fun createHttpClient(
    json: Json,
    extraConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient {
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    @Suppress("UNCHECKED_CAST")
    val typedConfig = extraConfig as HttpClientConfig<AndroidEngineConfig>.() -> Unit

    return HttpClient(Android) {
        engine {
            sslManager = { httpsURLConnection ->
                httpsURLConnection.sslSocketFactory = sslContext.socketFactory
                httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
        }
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

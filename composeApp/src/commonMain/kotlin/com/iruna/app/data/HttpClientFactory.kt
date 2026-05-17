package com.iruna.app.data

import io.ktor.client.*
import kotlinx.serialization.json.Json

/** Платформо-специфичный HTTP-клиент (Android — с кастомным SSL, iOS — стандартный Darwin).
 *  [extraConfig] — дополнительные плагины/настройки поверх базовых (ContentNegotiation + Logging).
 *
 *  Дефолтные значения намеренно вынесены в overload-обёртки ниже:
 *  default-лямбды в expect-параметрах вызывают баг IR-компилятора KMP
 *  (ExpectDeclarationRemover не может скопировать лямбду с JsonBuilder-ресивером). */
expect fun createHttpClient(
    json: Json,
    extraConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient

// ── Convenience overloads ─────────────────────────────────────────────────────

private val defaultJson = Json { ignoreUnknownKeys = true; isLenient = true }

fun createHttpClient(json: Json): HttpClient =
    createHttpClient(json) {}

fun createHttpClient(): HttpClient =
    createHttpClient(defaultJson) {}

fun createHttpClient(extraConfig: HttpClientConfig<*>.() -> Unit): HttpClient =
    createHttpClient(defaultJson, extraConfig)

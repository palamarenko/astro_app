package com.iruna.app.notifications

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Клиент для adminApi Cloud Function.
 *
 * Все вызовы: POST <functionUrl>
 *   Header: x-admin-secret: <adminSecret>
 *   Body:   { "action": "...", ...params }
 */
class PushAdminService(private val client: HttpClient) {

    companion object {
        /** Топик для ручной рассылки "всем сразу" (без учёта TZ). */
        const val TOPIC = "horoscope_daily"
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Отправляет пуш на топик horoscope_daily прямо сейчас (ручная рассылка). */
    suspend fun sendToAll(
        functionUrl: String,
        adminSecret: String,
        type: String = "daily_horoscope",
    ): Result<Unit> = runCatching {
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"sendPush","type":"$type"}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
    }

    /**
     * Загружает текущее расписание из Firestore.
     * Возвращает множество локальных часов (например {9, 18}).
     * Cloud Function сама конвертирует их в UTC-топики при рассылке.
     */
    suspend fun getSchedule(
        functionUrl: String,
        adminSecret: String,
    ): Result<Set<Int>> = runCatching {
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"getSchedule"}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["localHours"]
            ?.jsonArray
            ?.map { it.jsonPrimitive.int }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * Сохраняет расписание в Firestore.
     * [hours] — локальные часы (например {9, 18}).
     * Сервер разошлёт пуши в нужный TZ-топик, когда у их пользователей
     * наступит указанный час.
     */
    suspend fun setSchedule(
        functionUrl: String,
        adminSecret: String,
        hours: Set<Int>,
    ): Result<Unit> = runCatching {
        val hoursJson = hours.sorted().joinToString(",")
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"setSchedule","localHours":[$hoursJson]}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
    }

    /**
     * Запускает генерацию гороскопов через Cloud Function для указанной даты.
     * Если [lang] задан — генерирует только этот язык (12 знаков).
     * Если [lang] = null — генерирует все языки (устаревший режим).
     * Возвращает Pair(success, failed).
     */
    suspend fun generateHoroscopes(
        functionUrl: String,
        adminSecret: String,
        date: String,
        period: String = "daily",
        lang: String? = null,
    ): Result<Pair<Int, Int>> = runCatching {
        val requestBody = if (lang != null)
            """{"action":"generateHoroscopes","date":"$date","period":"$period","lang":"$lang"}"""
        else
            """{"action":"generateHoroscopes","date":"$date","period":"$period"}"""
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val success = body["success"]?.jsonPrimitive?.int ?: 0
        val failed  = body["failed"]?.jsonPrimitive?.int  ?: 0
        Pair(success, failed)
    }

    /** Загружает промпт из Firestore для указанного периода (или дефолтный если не задан). */
    suspend fun getPrompt(
        functionUrl: String,
        adminSecret: String,
        period: String = "daily",
    ): Result<String> = runCatching {
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"getPrompt","period":"$period"}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["prompt"]?.jsonPrimitive?.content ?: ""
    }

    /** Сохраняет промпт в Firestore для указанного периода. */
    suspend fun setPrompt(
        functionUrl: String,
        adminSecret: String,
        prompt: String,
        period: String = "daily",
    ): Result<Unit> = runCatching {
        val escaped = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"setPrompt","period":"$period","prompt":"$escaped"}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
    }

    /** Загружает расписание авто-генерации гороскопов (часы по UTC). */
    suspend fun getGenSchedule(
        functionUrl: String,
        adminSecret: String,
    ): Result<Set<Int>> = runCatching {
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"getGenSchedule"}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["localHours"]
            ?.jsonArray
            ?.map { it.jsonPrimitive.int }
            ?.toSet()
            ?: emptySet()
    }

    /** Сохраняет расписание авто-генерации гороскопов. */
    suspend fun setGenSchedule(
        functionUrl: String,
        adminSecret: String,
        hours: Set<Int>,
    ): Result<Unit> = runCatching {
        val hoursJson = hours.sorted().joinToString(",")
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"setGenSchedule","localHours":[$hoursJson]}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
    }

    /** Загружает языки, включённые для авто-генерации. */
    suspend fun getGenLangs(
        functionUrl: String,
        adminSecret: String,
    ): Result<Set<String>> = runCatching {
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"getGenLangs"}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["langs"]
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            ?.toSet()
            ?: emptySet()
    }

    /** Сохраняет языки, включённые для авто-генерации. */
    suspend fun setGenLangs(
        functionUrl: String,
        adminSecret: String,
        langs: Set<String>,
    ): Result<Unit> = runCatching {
        val langsJson = langs.joinToString(",") { "\"$it\"" }
        val response = client.post(functionUrl) {
            header("x-admin-secret", adminSecret)
            contentType(ContentType.Application.Json)
            setBody("""{"action":"setGenLangs","langs":[$langsJson]}""")
        }
        if (!response.status.isSuccess()) {
            error("Function error ${response.status.value}: ${response.bodyAsText()}")
        }
    }
}

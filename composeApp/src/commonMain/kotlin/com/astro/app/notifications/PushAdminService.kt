package com.astro.app.notifications

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
}

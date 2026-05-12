package com.astro.app.notifications

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Отправка push-уведомлений через Firebase Cloud Function.
 *
 * Cloud Function (functions/index.js) сама авторизуется через Firebase Admin SDK
 * и отправляет сообщение на топик "horoscope_daily".
 *
 * В приложении достаточно знать:
 *   - functionUrl  — URL задеплоенной функции
 *   - adminSecret  — статичный секрет (задаётся через `firebase functions:secrets:set ADMIN_SECRET`)
 *
 * URL функции после деплоя выглядит так:
 *   https://senddailyhoroscope-<hash>-uc.a.run.app
 * или (Gen 1 / region):
 *   https://us-central1-<project-id>.cloudfunctions.net/sendDailyHoroscope
 */
class PushAdminService(private val client: HttpClient) {

    companion object {
        const val TOPIC = "horoscope_daily"
    }

    /**
     * Отправляет data-сообщение на топик через Cloud Function.
     *
     * @param functionUrl  URL задеплоенной Firebase Cloud Function
     * @param adminSecret  значение секрета ADMIN_SECRET
     * @param type         тип сообщения (data payload)
     */
    suspend fun sendToAll(
        functionUrl: String,
        adminSecret: String,
        type: String = "daily_horoscope",
    ): Result<Unit> {
        return runCatching {
            val response = client.post(functionUrl) {
                header("x-admin-secret", adminSecret)
                contentType(ContentType.Application.Json)
                setBody("""{"type":"$type"}""")
            }
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                error("Function error ${response.status.value}: $body")
            }
        }
    }
}

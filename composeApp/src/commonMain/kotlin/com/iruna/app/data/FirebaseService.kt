package com.iruna.app.data

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json

class FirebaseService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = createHttpClient(json)

    private val baseUrl = "https://zodiac-b23ce-default-rtdb.europe-west1.firebasedatabase.app"

    // ── Horoscopes: read single sign ──────────────────────────────────────────
    suspend fun getHoroscope(lang: String, period: String, date: String, signId: String): HoroscopeResponse? {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period/$date/$signId.json"
            client.get(url).body<HoroscopeResponse?>()
        } catch (e: Exception) {
            null
        }
    }

    // ── Horoscopes: read all signs for a date node ────────────────────────────
    suspend fun getAllSignHoroscopes(lang: String, period: String, date: String): Map<String, HoroscopeResponse>? {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period/$date.json"
            client.get(url).body<Map<String, HoroscopeResponse>?>()
        } catch (e: Exception) {
            null
        }
    }

    // ── Horoscopes: write one sign ────────────────────────────────────────────
    suspend fun saveFullHoroscope(
        lang: String, period: String, date: String,
        signId: String, horoscope: HoroscopeResponse
    ): Boolean {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period/$date/$signId.json"
            val response = client.put(url) {
                contentType(ContentType.Application.Json)
                setBody(horoscope)
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    // ── Tarot: read all cards for a language ──────────────────────────────────
    suspend fun getAllTarotCards(lang: String): Map<String, TarotCardContent>? {
        return try {
            val url = "$baseUrl/tarot/$lang.json"
            client.get(url).body<Map<String, TarotCardContent>?>()
        } catch (e: Exception) {
            null
        }
    }

    // ── Tarot: write one card ─────────────────────────────────────────────────
    suspend fun saveTarotCard(lang: String, cardKey: String, content: TarotCardContent): Boolean {
        return try {
            val url = "$baseUrl/tarot/$lang/$cardKey.json"
            val response = client.put(url) {
                contentType(ContentType.Application.Json)
                setBody(content)
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    // ── Generation logs ───────────────────────────────────────────────────────

    /** Сохраняет запись лога генерации. Ключ = timestamp. */
    suspend fun saveGenerationLog(entry: GenerationLogEntry): Boolean {
        return try {
            val url = "$baseUrl/generation_logs/${entry.id}.json"
            val response = client.put(url) {
                contentType(ContentType.Application.Json)
                setBody(entry)
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /** Загружает все записи лога, возвращает последние [limit] отсортированных по убыванию.
     *  Ключи — строки timestamp (мс), поэтому $key сортирует их правильно без доп. индекса. */
    suspend fun getGenerationLogs(limit: Int = 30): List<GenerationLogEntry> {
        return try {
            val url = "$baseUrl/generation_logs.json?orderBy=%22%24key%22&limitToLast=$limit"
            val raw = client.get(url).body<Map<String, GenerationLogEntry>?>()
            raw?.values?.sortedByDescending { it.timestamp } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Возвращает количество знаков, сохранённых для конкретного dateKey.
     *  Использует shallow=true — загружает только ключи знаков, без текста. */
    suspend fun getSignCountForDateKey(lang: String, period: String, dateKey: String): Int {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period/$dateKey.json?shallow=true"
            val raw = client.get(url).body<Map<String, Boolean>?>()
            raw?.size ?: 0
        } catch (e: Exception) { 0 }
    }

    /** Возвращает набор dateKey-ключей (дни/недели/месяцы) для которых есть гороскопы.
     *  Использует shallow=true — загружает только ключи, без данных. */
    suspend fun getAvailableDateKeys(lang: String, period: String): Set<String> {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period.json?shallow=true"
            val raw = client.get(url).body<Map<String, Boolean>?>()
            raw?.keys ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** Удаляет все гороскопы для указанного dateKey во всех языках (ru, uk, en). */
    suspend fun deleteAllLangsDateKey(period: String, dateKey: String): Boolean {
        return try {
            listOf("ru", "uk", "en").forEach { lang ->
                client.delete("$baseUrl/horoscopes/$lang/$period/$dateKey.json")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Horoscope metadata ────────────────────────────────────────────────────

    /** Записывает метадату: сколько знаков заполнено для lang/period/dateKey. */
    suspend fun saveHoroscopeMeta(lang: String, period: String, dateKey: String, count: Int): Boolean {
        return try {
            val url = "$baseUrl/meta/$lang/$period/$dateKey.json"
            client.put(url) {
                contentType(ContentType.Application.Json)
                setBody(HoroscopeMeta(count = count, savedAt = Clock.System.now().toEpochMilliseconds()))
            }
            true
        } catch (e: Exception) { false }
    }

    /** Загружает метадату: возвращает Map<dateKey, count> для lang/period. */
    suspend fun getHoroscopeMeta(lang: String, period: String): Map<String, Int> {
        return try {
            val url = "$baseUrl/meta/$lang/$period.json"
            val raw = client.get(url).body<Map<String, HoroscopeMeta>?>()
            raw?.mapValues { it.value.count } ?: emptyMap()
        } catch (e: Exception) { emptyMap() }
    }

    /** Удаляет метадату для dateKey во всех языках. */
    suspend fun deleteHoroscopeMeta(period: String, dateKey: String): Boolean {
        return try {
            listOf("ru", "uk", "en").forEach { lang ->
                client.delete("$baseUrl/meta/$lang/$period/$dateKey.json")
            }
            true
        } catch (e: Exception) { false }
    }
}

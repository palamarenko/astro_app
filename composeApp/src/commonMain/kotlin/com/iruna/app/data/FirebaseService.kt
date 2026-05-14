package com.iruna.app.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class FirebaseService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level  = LogLevel.ALL
        }
    }

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
}

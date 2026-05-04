package com.astro.app.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
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
    }
    
    private val baseUrl = "https://zodiac-b23ce-default-rtdb.europe-west1.firebasedatabase.app"

    suspend fun getHoroscope(lang: String, period: String, date: String, signId: String): HoroscopeResponse? {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period/$date/$signId.json"
            client.get(url).body<HoroscopeResponse>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveHoroscope(lang: String, period: String, date: String, signId: String, text: String): Boolean {
        return try {
            val url = "$baseUrl/horoscopes/$lang/$period/$date/$signId.json"
            val response = client.put(url) {
                setBody(text)
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

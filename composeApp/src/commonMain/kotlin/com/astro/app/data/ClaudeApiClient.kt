package com.astro.app.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class AnthropicMessage(val role: String, val content: String)
@Serializable data class AnthropicRequest(
    val model: String = "claude-haiku-4-5",
    val max_tokens: Int = 512,
    val messages: List<AnthropicMessage>,
)
@Serializable data class ContentBlock(val type: String, val text: String = "")
@Serializable data class AnthropicResponse(val content: List<ContentBlock>)

class ClaudeApiClient(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient { install(ContentNegotiation) { json(json) } }

    private suspend fun complete(prompt: String, maxTokens: Int = 512): String {
        val response: AnthropicResponse = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(AnthropicRequest(max_tokens = maxTokens, messages = listOf(AnthropicMessage("user", prompt))))
        }.body()
        return response.content.firstOrNull()?.text ?: ""
    }

    // periodPrompt — localized string, e.g. "на сегодня" / "for today" / "на сьогодні"
    suspend fun getHoroscope(sign: ZodiacSign, periodPrompt: String): HoroscopeResponse {
        val prompt = """
            Horoscope for "${sign.name}" (${sign.element}) $periodPrompt.
            Respond ONLY with JSON, no markdown:
            {"text":"3-4 poetic sentences","keyword":"1-2 words","love":72,"career":85,"health":60,"energy":78}
        """.trimIndent()
        return json.decodeFromString(complete(prompt).trim())
    }

    suspend fun getCompatibility(sign1: ZodiacSign, sign2: ZodiacSign): CompatibilityResponse {
        val prompt = """
            Astrological compatibility of "${sign1.name}" and "${sign2.name}".
            Respond ONLY with JSON, no markdown:
            {"score":82,"title":"3-5 word title","text":"2-3 poetic sentences","strengths":"1 sentence","challenges":"1 sentence"}
        """.trimIndent()
        return json.decodeFromString(complete(prompt).trim())
    }

    suspend fun getTarotReading(cards: List<TarotCard>): TarotReadingResponse {
        val positions = listOf("Past", "Present", "Future")
        val cardDesc = cards.mapIndexed { i, c ->
            "${positions[i]}: ${c.name}${if (c.reversed) " (reversed)" else ""}"
        }.joinToString(", ")
        val prompt = """
            Three-card Tarot spread — $cardDesc.
            Respond ONLY with JSON, no markdown:
            {"past":"1-2 sentences","present":"1-2 sentences","future":"1-2 sentences","summary":"1-2 sentences overall reading"}
        """.trimIndent()
        return json.decodeFromString(complete(prompt, maxTokens = 600).trim())
    }

    suspend fun getSignInsight(sign: ZodiacSign): String {
        val prompt = """
            Brief astrological insight for "${sign.name}":
            character traits, life mission, relationship style.
            2-3 poetic sentences. Plain text only, no headings.
        """.trimIndent()
        return complete(prompt, maxTokens = 256)
    }
}

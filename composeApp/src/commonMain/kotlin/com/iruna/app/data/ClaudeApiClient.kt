package com.iruna.app.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class TarotPersonalContext(
    val name: String? = null,
    val gender: String? = null,     // "male" | "female"
    val birthDate: String? = null,
    val birthSign: String? = null,
    val birthPlace: String? = null,
)

@Serializable data class AnthropicMessage(val role: String, val content: String)
@Serializable data class AnthropicRequest(
    @SerialName("model") val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
)
@Serializable data class ContentBlock(val type: String, val text: String = "")
@Serializable data class AnthropicResponse(val content: List<ContentBlock> = emptyList())

private const val MODEL = "claude-haiku-4-5-20251001"

class ClaudeApiClient(private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor: $message")
                }
            }
            level = LogLevel.ALL
        }
    }

    // Strip optional ```json ... ``` fences the model sometimes adds despite instructions
    private fun extractJson(raw: String): String {
        val s = raw.trim()
        if (!s.startsWith("```")) return s
        return s
            .removePrefix("```json")
            .removePrefix("```")
            .trimStart('\n', '\r')
            .substringBeforeLast("```")
            .trim()
    }

    private suspend fun complete(prompt: String, maxTokens: Int = 512): String {
        val httpResp = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(AnthropicRequest(model = MODEL, maxTokens = maxTokens, messages = listOf(AnthropicMessage("user", prompt))))
        }
        if (!httpResp.status.isSuccess()) {
            val body = httpResp.bodyAsText()
            throw Exception("Anthropic ${httpResp.status.value}: $body")
        }
        val response: AnthropicResponse = httpResp.body()
        return response.content.firstOrNull()?.text ?: ""
    }

    suspend fun getHoroscope(sign: ZodiacSign, periodPrompt: String, lang: String = "ru"): HoroscopeResponse {
        val langName = when (lang) { "uk" -> "Ukrainian"; "en" -> "English"; else -> "Russian" }
        val prompt = """
            Horoscope for "${sign.name}" (${sign.element}) $periodPrompt.
            Language: $langName.
            Scores range 50–100 where 50=bad, 75=average, 100=perfect.
            The text must reflect the score levels (low scores → warnings/caution, high → optimism/praise).
            Respond ONLY with JSON, no markdown:
            {"text":"3-4 poetic sentences","keyword":"1-2 words","love":72,"career":85,"health":60,"energy":78}
        """.trimIndent()
        return json.decodeFromString(extractJson(complete(prompt)))
    }

    suspend fun getCompatibility(sign1: ZodiacSign, sign2: ZodiacSign, lang: String = "ru"): CompatibilityResponse {
        val langName = when (lang) {
            "uk" -> "Ukrainian"
            "en" -> "English"
            else -> "Russian"
        }
        val prompt = """
            Astrological compatibility of "${sign1.name}" and "${sign2.name}".
            Language: $langName.
            Respond ONLY with JSON, no markdown:
            {"score":82,"title":"3-5 word title","text":"2-3 poetic sentences","strengths":"1 sentence","challenges":"1 sentence"}
        """.trimIndent()
        return json.decodeFromString(extractJson(complete(prompt)))
    }



    /** Generates only the overall summary for a three-card spread.
     *  The individual card texts (past/present/future) come from Firebase. */
    suspend fun getTarotSummary(
        cards: List<TarotCard>,
        context: TarotPersonalContext? = null,
        lang: String = "ru",
    ): String {
        val langName = when (lang) {
            "uk" -> "Ukrainian"
            "en" -> "English"
            else -> "Russian"
        }
        val positions = when (lang) {
            "uk" -> listOf("Минуле", "Теперішнє", "Майбутнє")
            "en" -> listOf("Past", "Present", "Future")
            else -> listOf("Прошлое", "Настоящее", "Будущее")
        }
        val reversedLabel = when (lang) {
            "uk" -> "перевернута"
            "en" -> "reversed"
            else -> "перевёрнутая"
        }

        val cardDesc = cards.mapIndexed { i, c ->
            val rev = if (c.reversed) " ($reversedLabel)" else ""
            "${positions[i]}: ${c.name}$rev"
        }.joinToString("; ")

        val genderHint = when (context?.gender) {
            "male"   -> when (lang) {
                "uk" -> "Стать: чоловіча — використовуй чоловічий рід у зверненні."
                "en" -> "Gender: male — address the seeker using masculine forms."
                else -> "Пол: мужской — обращайся к человеку в мужском роде."
            }
            "female" -> when (lang) {
                "uk" -> "Стать: жіноча — використовуй жіночий рід у зверненні."
                "en" -> "Gender: female — address the seeker using feminine forms."
                else -> "Пол: женский — обращайся к человеку в женском роде."
            }
            else -> ""
        }

        val contextBlock = if (context != null) {
            val parts = buildList {
                context.name?.let { add("имя: $it") }
                context.birthDate?.let { add("дата рождения: $it") }
                context.birthSign?.let { add("знак зодиака: $it") }
                context.birthPlace?.let { add("место рождения: $it") }
            }
            if (parts.isNotEmpty())
                "\nSeekerʼs personal context (weave it naturally where meaningful, do not force it): ${parts.joinToString(", ")}."
            else ""
        } else ""

        val genderLine = if (genderHint.isNotEmpty()) "\n$genderHint" else ""

        val prompt = """
            Three-card Tarot spread: $cardDesc.$contextBlock$genderLine
            Reversed cards carry the archetype's energy turned inward or blocked.
            Write a 2-3 sentence overall reading summary grounded in the next 1-2 days — today and tomorrow.
            Speak directly to what the person may experience, feel, or face in the nearest hours ahead.
            Language: $langName.
            Style: warm and light — like a trusted friend sharing an insight, not a formal oracle.
            Keep the mystical feel but stay conversational: no heavy metaphors, no dramatic flourishes.
            Plain text only — no JSON, no markdown.
        """.trimIndent()

        return complete(prompt, maxTokens = 300)
    }

    suspend fun getSignInsight(sign: ZodiacSign): String {
        val prompt = """
            Brief astrological insight for "${sign.name}":
            character traits, life mission, relationship style.
            2-3 poetic sentences. Plain text only, no headings.
        """.trimIndent()
        return complete(prompt, maxTokens = 256)
    }

    // Admin: generate tarot card interpretations in the target language
    suspend fun generateAdminTarotCard(
        card: TarotCard,
        lang: String,
    ): TarotCardContent {
        val langName = when (lang) { "en" -> "English"; "uk" -> "Ukrainian"; else -> "Russian" }
        val prompt = """
            Write tarot card interpretations for the Major Arcana card "${card.name}" (${card.number}), keywords: ${card.keywords}.
            Language: $langName. Style: poetic, mystical, personal, inspiring.
            Context: "past" — how this card's energy manifested in the past; "present" — current situation; "future" — what awaits ahead.
            Each field: exactly ~20 words, one concise sentence. Do NOT repeat keywords verbatim.
            Respond ONLY with valid JSON, no markdown:
            {"past":"...","present":"...","future":"..."}
        """.trimIndent()
        return json.decodeFromString(extractJson(complete(prompt, maxTokens = 200)))
    }

    // Admin: generate a horoscope in the target language for a specific period
    suspend fun generateAdminHoroscope(
        sign: ZodiacSign,
        period: HoroscopePeriod,
        lang: String,
        dateKey: String,
    ): HoroscopeResponse {
        val langName = when (lang) { "en" -> "English"; "uk" -> "Ukrainian"; else -> "Russian" }
        val periodDesc = when (period) {
            HoroscopePeriod.DAILY   -> "for the day ($dateKey)"
            HoroscopePeriod.WEEKLY  -> "for the week ($dateKey)"
            HoroscopePeriod.MONTHLY -> "for the month ($dateKey)"
        }
        val prompt = """
            Write a horoscope for zodiac sign "${sign.name}" (element: ${sign.element}, planet: ${sign.planet}) $periodDesc.
            Language: $langName. Style: poetic, inspiring, mystical.
            Text: 3-4 sentences. Scores: integers from 50 to 100 (50=bad, 75=average, 100=perfect).
            The text must reflect the score levels — low scores should hint at caution or challenges, high scores at success and joy.
            Respond ONLY with valid JSON, no markdown:
            {"text":"...","keyword":"1-2 words","love":72,"career":85,"health":60,"energy":78}
        """.trimIndent()
        return json.decodeFromString(extractJson(complete(prompt, maxTokens = 450)))
    }
}

package com.iruna.app.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class AnthropicMessage(val role: String, val content: String)
@Serializable data class AnthropicRequest(
    @SerialName("model") val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
)
@Serializable data class ContentBlock(val type: String, val text: String = "")
@Serializable data class AnthropicResponse(val content: List<ContentBlock> = emptyList())

private const val ANTHROPIC_MODEL = "claude-haiku-4-5-20251001"

/**
 * Реализация [AiGenerationService] через Anthropic Claude API.
 *
 * Автоматически повторяет запрос при ошибке 529 (overloaded) — до 3 попыток
 * с нарастающей задержкой 3 / 6 сек.
 */
class AnthropicAiProvider(private val apiKey: String) : AiGenerationService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client: HttpClient = createHttpClient(json)

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** ISO-код → название языка для промптов. */
    private fun langName(lang: String) = when (lang) {
        "uk" -> "Ukrainian"
        "en" -> "English"
        "es" -> "Spanish"
        "de" -> "German"
        "fr" -> "French"
        else -> "Russian"
    }

    /** Убирает опциональные ```json … ``` обёртки. */
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

    /** Если модель обернула ответ в ключ верхнего уровня — разворачиваем. */
    private fun unwrapHoroscopeJson(raw: String): String {
        return try {
            val el = json.parseToJsonElement(raw)
            if (el !is kotlinx.serialization.json.JsonObject) return raw
            if (el.containsKey("text")) return raw
            val nested = el.values.filterIsInstance<kotlinx.serialization.json.JsonObject>()
                .firstOrNull { it.containsKey("text") }
            nested?.toString() ?: raw
        } catch (_: Exception) { raw }
    }

    /**
     * Базовый HTTP-запрос к Anthropic.
     * При ошибке 529 (overloaded) — до 3 попыток с задержкой 3 / 6 сек.
     */
    private suspend fun complete(prompt: String, maxTokens: Int = 512): String {
        val maxAttempts = 3
        var lastError: Exception = Exception("Unknown error")

        repeat(maxAttempts) { attempt ->
            val httpResp = client.post("https://api.anthropic.com/v1/messages") {
                contentType(ContentType.Application.Json)
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                setBody(
                    AnthropicRequest(
                        model     = ANTHROPIC_MODEL,
                        maxTokens = maxTokens,
                        messages  = listOf(AnthropicMessage("user", prompt)),
                    )
                )
            }
            when {
                httpResp.status.value == 529 -> {
                    // Перегруженность — ждём и повторяем
                    lastError = Exception("Anthropic overloaded (529), attempt ${attempt + 1}/$maxAttempts")
                    if (attempt < maxAttempts - 1) delay(3000L * (attempt + 1))
                }
                !httpResp.status.isSuccess() -> {
                    // Другие HTTP-ошибки — бросаем сразу
                    val body = httpResp.bodyAsText()
                    throw Exception("Anthropic ${httpResp.status.value}: $body")
                }
                else -> {
                    val response: AnthropicResponse = httpResp.body()
                    return response.content.firstOrNull()?.text ?: ""
                }
            }
        }
        throw lastError
    }

    // ── AiGenerationService impl ──────────────────────────────────────────────

    override suspend fun getHoroscope(sign: ZodiacSign, periodPrompt: String, lang: String): HoroscopeResponse {
        val langName = langName(lang)
        val prompt = """
            Horoscope for "${sign.name}" (${sign.element}) $periodPrompt.
            Language: $langName.
            Scores range 50–100 where 50=bad, 75=average, 100=perfect.
            The text must reflect the score levels (low scores → warnings/caution, high → optimism/praise).
            IMPORTANT: Respond ONLY with this exact flat JSON structure, no markdown, no extra keys, no nesting:
            {"text":"3-4 poetic sentences","keyword":"1-2 words","love":72,"career":85,"health":60,"energy":78}
        """.trimIndent()
        return json.decodeFromString(unwrapHoroscopeJson(extractJson(complete(prompt))))
    }

    override suspend fun getCompatibility(sign1: ZodiacSign, sign2: ZodiacSign, lang: String): CompatibilityResponse {
        val langName = langName(lang)
        val prompt = """
            Astrological compatibility of "${sign1.name}" and "${sign2.name}".
            Language: $langName.
            Respond ONLY with JSON, no markdown:
            {"score":82,"title":"3-5 word title","text":"2-3 poetic sentences","strengths":"1 sentence","challenges":"1 sentence"}
        """.trimIndent()
        return json.decodeFromString(extractJson(complete(prompt)))
    }

    override suspend fun getTarotSummary(
        cards: List<TarotCard>,
        context: TarotPersonalContext?,
        lang: String,
    ): String {
        val langName = langName(lang)
        val positions = when (lang) {
            "uk" -> listOf("Минуле", "Теперішнє", "Майбутнє")
            "en" -> listOf("Past", "Present", "Future")
            "es" -> listOf("Pasado", "Presente", "Futuro")
            "de" -> listOf("Vergangenheit", "Gegenwart", "Zukunft")
            "fr" -> listOf("Passé", "Présent", "Futur")
            else -> listOf("Прошлое", "Настоящее", "Будущее")
        }
        val reversedLabel = when (lang) {
            "uk" -> "перевернута"
            "en" -> "reversed"
            "es" -> "invertida"
            "de" -> "umgekehrt"
            "fr" -> "renversé"
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
                "es" -> "Género: masculino — dirígete al consultante usando formas masculinas."
                "de" -> "Geschlecht: männlich — verwende männliche Anredeformen."
                "fr" -> "Genre: masculin — adresse-toi au consultant en utilisant des formes masculines."
                else -> "Пол: мужской — обращайся к человеку в мужском роде."
            }
            "female" -> when (lang) {
                "uk" -> "Стать: жіноча — використовуй жіночий рід у зверненні."
                "en" -> "Gender: female — address the seeker using feminine forms."
                "es" -> "Género: femenino — dirígete a la consultante usando formas femeninas."
                "de" -> "Geschlecht: weiblich — verwende weibliche Anredeformen."
                "fr" -> "Genre: féminin — adresse-toi à la consultante en utilisant des formes féminines."
                else -> "Пол: женский — обращайся к человеку в женском роде."
            }
            else -> ""
        }

        val contextBlock = if (context != null) {
            val parts = buildList {
                context.name?.let      { add("name: $it") }
                context.birthDate?.let { add("date of birth: $it") }
                context.birthSign?.let { add("zodiac sign: $it") }
                context.birthPlace?.let { add("place of birth: $it") }
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

    override suspend fun getSignInsight(sign: ZodiacSign): String {
        val prompt = """
            Brief astrological insight for "${sign.name}":
            character traits, life mission, relationship style.
            2-3 poetic sentences. Plain text only, no headings.
        """.trimIndent()
        return complete(prompt, maxTokens = 256)
    }

    override suspend fun generateAdminHoroscope(
        sign: ZodiacSign,
        period: HoroscopePeriod,
        lang: String,
        dateKey: String,
        styleInstructions: String?,
    ): HoroscopeResponse {
        val langName = langName(lang)
        val periodDesc = when (period) {
            HoroscopePeriod.DAILY   -> "for the day ($dateKey)"
            HoroscopePeriod.WEEKLY  -> "for the week ($dateKey)"
            HoroscopePeriod.MONTHLY -> "for the month ($dateKey)"
        }
        val weeklyNote = if (period == HoroscopePeriod.WEEKLY)
            "\nDo NOT mention week numbers (e.g. 'week 32' or 'W32'). " +
            "Instead refer to the time naturally, e.g. 'the second week of August', 'mid-July', 'the last days of October'."
        else ""
        val style = styleInstructions?.takeIf { it.isNotBlank() }?.plus(weeklyNote) ?:
            "Style: warm, clear, easy to read — like advice from a trusted friend, not a mystical oracle.\n" +
            "Avoid heavy metaphors, flowery language, and vague cosmic imagery. Write in plain, natural sentences.\n" +
            "Text: 6-8 short sentences. Include at least one concrete prediction or practical tip. " +
            "Scores: integers from 50 to 100 (50=bad, 75=average, 100=perfect). " +
            "The text must reflect the scores — name specific challenges for low scores, specific opportunities for high scores." +
            weeklyNote + "\n" +
            "For Ukrainian: write in authentic literary Ukrainian. Avoid russicisms and calques from Russian. " +
            "Use native Ukrainian vocabulary and phrasing — the text must feel natural to a native Ukrainian speaker, " +
            "not like a translation from Russian.\n" +
            "For Russian: use expressive literary Russian.\n" +
            "For English: use poetic but accessible English.\n" +
            "For Spanish: write in natural, warm Spanish. Use smooth, conversational phrasing that feels native.\n" +
            "For German: write in clear, warm, modern German. Avoid overly formal tone.\n" +
            "For French: write in elegant, natural French. The tone should feel warm and literary."
        val prompt = """
            Write a horoscope for zodiac sign "${sign.name}" (element: ${sign.element}, planet: ${sign.planet}) $periodDesc.
            Language: $langName.
            $style
            IMPORTANT: Respond ONLY with this exact flat JSON structure, no markdown, no extra keys, no nesting:
            {"text":"...","keyword":"1-2 words","love":72,"career":85,"health":60,"energy":78}
        """.trimIndent()
        return json.decodeFromString(unwrapHoroscopeJson(extractJson(complete(prompt, maxTokens = 900))))
    }

    override suspend fun generateAdminAllSigns(
        signs: List<ZodiacSign>,
        period: HoroscopePeriod,
        lang: String,
        dateKey: String,
        styleInstructions: String?,
    ): Map<String, HoroscopeResponse> {
        val langName   = langName(lang)
        val periodWord = when (period) {
            HoroscopePeriod.DAILY   -> "daily"
            HoroscopePeriod.WEEKLY  -> "weekly"
            HoroscopePeriod.MONTHLY -> "monthly"
        }
        val periodDesc = when (period) {
            HoroscopePeriod.DAILY   -> "day ($dateKey)"
            HoroscopePeriod.WEEKLY  -> "week ($dateKey)"
            HoroscopePeriod.MONTHLY -> "month ($dateKey)"
        }
        val weeklyNote = if (period == HoroscopePeriod.WEEKLY)
            "\nDo NOT mention week numbers (e.g. 'week 32'). Refer to time naturally, e.g. 'mid-July'."
        else ""
        val style = styleInstructions?.takeIf { it.isNotBlank() }?.plus(weeklyNote) ?:
            "Style: warm, clear, easy to read — like advice from a trusted friend, not a mystical oracle.\n" +
            "Avoid heavy metaphors. Write in plain, natural sentences.\n" +
            "Text: 6-8 short sentences. Include at least one concrete prediction or practical tip.\n" +
            "Scores: integers 50–100 (50=bad, 75=average, 100=perfect). Reflect scores in the text." +
            weeklyNote + "\n" +
            "For Ukrainian: authentic literary Ukrainian, no russicisms.\n" +
            "For Russian: expressive literary Russian.\n" +
            "For English: poetic but accessible English.\n" +
            "For Spanish: natural, warm Spanish.\n" +
            "For German: clear, warm, modern German.\n" +
            "For French: elegant, natural French."
        val signsDesc     = signs.joinToString("; ") { "${it.id} (${it.name}, ${it.element}, ${it.planet})" }
        val schemaExample = signs.first().id
        val prompt = """
            Generate $periodWord horoscopes for all 12 zodiac signs for the $periodDesc.
            Language: $langName.

            $style

            Signs: $signsDesc

            IMPORTANT: Respond ONLY with a valid flat JSON object — no markdown, no extra text:
            {"$schemaExample":{"text":"...","keyword":"1-2 words","love":72,"career":85,"health":60,"energy":78},"taurus":{...},...}
            Include all 12 signs. Each "text" must be in $langName.
        """.trimIndent()

        val raw    = complete(prompt, maxTokens = 6000)
        val parsed = json.parseToJsonElement(extractJson(raw))
        if (parsed !is kotlinx.serialization.json.JsonObject) return emptyMap()

        return signs.mapNotNull { sign ->
            val obj = parsed[sign.id] as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            try {
                val response = json.decodeFromString<HoroscopeResponse>(obj.toString())
                sign.id to response
            } catch (_: Exception) { null }
        }.toMap()
    }

    override suspend fun getDreamInterpretation(dreamText: String, lang: String): String {
        val langName = langName(lang)
        val langNote = when (lang) {
            "uk" -> "Write exclusively in Ukrainian. Use authentic literary Ukrainian — no russicisms, no calques from Russian. The text must feel natural to a native Ukrainian speaker."
            "ru" -> "Write exclusively in Russian. Use expressive literary Russian."
            "en" -> "Write exclusively in English. Use poetic but accessible English."
            "es" -> "Write exclusively in Spanish. Use natural, warm Spanish that feels native."
            "de" -> "Write exclusively in German. Use clear, warm, modern German."
            "fr" -> "Write exclusively in French. Use elegant, natural French with a warm literary tone."
            else -> "Write exclusively in Russian."
        }
        val prompt = """
            You are a dream interpreter who combines symbolism, psychology, and mystical insight.
            A person described this dream: "$dreamText"
            $langNote
            Write a warm, personal, insightful interpretation of this dream in 3-5 sentences.
            Cover: key symbols, emotional meaning, and a gentle guidance message.
            Style: mystical yet accessible, like a wise friend — not overly formal.
            IMPORTANT: Your entire response must be in $langName only. Do not use any other language.
            Plain text only, no JSON, no markdown, no headings.
        """.trimIndent()
        return complete(prompt, maxTokens = 400)
    }

    override suspend fun generateAdminTarotCard(card: TarotCard, lang: String): TarotCardContent {
        val langName = langName(lang)
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
}

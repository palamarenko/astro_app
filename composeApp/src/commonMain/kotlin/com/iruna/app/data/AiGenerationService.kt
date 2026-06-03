package com.iruna.app.data

/**
 * Абстрактный интерфейс для генерации контента через ИИ.
 * Реализации: [AnthropicAiProvider] (Claude), + Gemini / ChatGPT при необходимости.
 */
interface AiGenerationService {

    // ── Пользовательские функции ──────────────────────────────────────────────

    suspend fun getHoroscope(
        sign: ZodiacSign,
        periodPrompt: String,
        lang: String,
    ): HoroscopeResponse

    suspend fun getCompatibility(
        sign1: ZodiacSign,
        sign2: ZodiacSign,
        lang: String,
    ): CompatibilityResponse

    suspend fun getTarotSummary(
        cards: List<TarotCard>,
        context: TarotPersonalContext? = null,
        lang: String,
    ): String

    suspend fun getSignInsight(sign: ZodiacSign): String

    suspend fun getDreamInterpretation(dreamText: String, lang: String): String

    // ── Административные функции ──────────────────────────────────────────────

    /** Генерирует гороскоп для одного знака. */
    suspend fun generateAdminHoroscope(
        sign: ZodiacSign,
        period: HoroscopePeriod,
        lang: String,
        dateKey: String,
        styleInstructions: String? = null,
    ): HoroscopeResponse

    /** Генерирует гороскопы для всех 12 знаков одним запросом.
     *  Возвращает Map signId → HoroscopeResponse (отсутствующие = пустой текст). */
    suspend fun generateAdminAllSigns(
        signs: List<ZodiacSign>,
        period: HoroscopePeriod,
        lang: String,
        dateKey: String,
        styleInstructions: String? = null,
    ): Map<String, HoroscopeResponse>

    /** Генерирует интерпретации карты Таро. */
    suspend fun generateAdminTarotCard(
        card: TarotCard,
        lang: String,
    ): TarotCardContent
}

// ── Контекст пользователя для расклада Таро ───────────────────────────────────

data class TarotPersonalContext(
    val name: String? = null,
    val gender: String? = null,     // "male" | "female"
    val birthDate: String? = null,
    val birthSign: String? = null,
    val birthPlace: String? = null,
)

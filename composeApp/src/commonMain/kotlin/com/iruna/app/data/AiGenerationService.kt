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

    /** Генерирует прогноз «Карты дня» для одной таро-карты. */
    suspend fun generateAdminDayCard(
        card: TarotCard,
        lang: String,
    ): DayCardContent

    /** Возвращает информацию об использовании API (токены, баланс). */
    suspend fun getBillingInfo(): AnthropicUsageInfo
}

// ── Биллинг ───────────────────────────────────────────────────────────────────

data class AnthropicUsageInfo(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheReadTokens: Long = 0,
    val cacheCreationTokens: Long = 0,
    /** Примерная стоимость в USD на основе цен Claude Haiku */
    val estimatedCostUsd: Double = 0.0,
    /** Сырой JSON-ответ от API (для отладки) */
    val rawResponse: String = "",
)

// ── Контекст пользователя для расклада Таро ───────────────────────────────────

data class TarotPersonalContext(
    val name: String? = null,
    val gender: String? = null,     // "male" | "female"
    val birthDate: String? = null,
    val birthSign: String? = null,
    val birthPlace: String? = null,
)

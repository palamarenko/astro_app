package com.iruna.app.ui.screens.admin

import com.iruna.app.data.*
import com.iruna.app.data.AnthropicUsageInfo
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** Все языки, поддерживаемые генерацией гороскопов (code → label). */
internal val ALL_GEN_LANG_LABELS: List<Pair<String, String>> =
    listOf("ru" to "RU", "uk" to "UK", "en" to "EN", "es" to "ES", "de" to "DE", "fr" to "FR", "ar" to "AR")

/** Коды всех языков генерации в порядке отображения. */
internal val ALL_GEN_LANGS: List<String> = ALL_GEN_LANG_LABELS.map { it.first }

/** Полное покрытие даты = 12 знаков × число языков генерации. */
internal val CALENDAR_FULL_COUNT: Int = ALL_GEN_LANGS.size * 12

internal fun computeDateKey(period: HoroscopePeriod, date: LocalDate): String = when (period) {
    HoroscopePeriod.DAILY   -> date.toString()
    HoroscopePeriod.WEEKLY  -> {
        val week = (date.dayOfYear / 7) + 1
        "${date.year}-W${week.toString().padStart(2, '0')}"
    }
    HoroscopePeriod.MONTHLY -> "${date.year}-${date.monthNumber.toString().padStart(2, '0')}"
}

internal fun defaultHoroscopes(): Map<String, HoroscopeResponse> =
    ALL_SIGNS.associate { it.id to HoroscopeResponse(text = "", love = 75, career = 75, health = 75, energy = 75) }

data class AdminUiState(
    val lang: String = "ru",
    val period: HoroscopePeriod = HoroscopePeriod.DAILY,
    val selectedDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val horoscopes: Map<String, HoroscopeResponse> = defaultHoroscopes(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = -1,
    val saveError: String? = null,
    val loadError: String? = null,
    val isLoaded: Boolean = false,
    val generatingSignIds: Set<String> = emptySet(),
    val isGeneratingAll: Boolean = false,
    val generateError: String? = null,
    // ── Push notifications ────────────────────────────────────────────────────
    val functionUrl: String = "",
    val adminSecret: String = "",
    val pushSending: Boolean = false,
    val pushResult: String? = null,
    // ── Schedule ──────────────────────────────────────────────────────────────
    val scheduleHours: Set<Int> = emptySet(),
    val scheduleLoading: Boolean = false,
    val scheduleSaving: Boolean = false,
    val scheduleSaved: Boolean = false,
    val scheduleError: String? = null,
    // ── Generate All Languages ────────────────────────────────────────────────
    val genAllLangsLoading: Boolean = false,
    val genAllLangsResult: String? = null,
    val genAllLangsPeriod: HoroscopePeriod = HoroscopePeriod.DAILY,
    val genAllLangsDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    /** Языки, выбранные для генерации. По умолчанию все. */
    val genAllLangsSelected: Set<String> = ALL_GEN_LANGS.toSet(),
    /** Текущий язык в процессе генерации, например "es" (null = не генерируется). */
    val genAllLangsCurrentLang: String? = null,
    /** Сколько языков уже обработано (успешно или с ошибкой). */
    val genAllLangsDone: Int = 0,
    // ── Horoscope Prompt ──────────────────────────────────────────────────────
    val promptText: String = "",
    val promptLoading: Boolean = false,
    val promptSaving: Boolean = false,
    val promptSaved: Boolean = false,
    val promptError: String? = null,
    // ── Generation schedule ───────────────────────────────────────────────────
    val genScheduleHours: Set<Int> = emptySet(),
    val genScheduleLoading: Boolean = false,
    val genScheduleSaving: Boolean = false,
    val genScheduleSaved: Boolean = false,
    val genScheduleError: String? = null,
    // ── Auto-generation languages ─────────────────────────────────────────────
    /** Языки, включённые для авто-генерации по расписанию. По умолчанию все. */
    val genLangsEnabled: Set<String> = ALL_GEN_LANGS.toSet(),
    val genLangsLoading: Boolean = false,
    val genLangsSaving: Boolean = false,
    val genLangsSaved: Boolean = false,
    val genLangsError: String? = null,
    // ── Generation logs ───────────────────────────────────────────────────────
    val generationLogs: List<GenerationLogEntry> = emptyList(),
    val logsLoading: Boolean = false,
    // ── Calendar ──────────────────────────────────────────────────────────────
    val calendarPeriod: HoroscopePeriod = HoroscopePeriod.DAILY,
    val calendarViewYear: Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year,
    val calendarViewMonth: Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).monthNumber,
    val calendarMeta: Map<String, Int> = emptyMap(),
    /** Мета по каждому языку отдельно: lang → (dateKey → count 0..12). */
    val calendarMetaByLang: Map<String, Map<String, Int>> = emptyMap(),
    /** dateKey ячейки, для которой открыт попап с деталями (null = закрыт). */
    val calendarPopupKey: String? = null,
    /** Язык, который сейчас удаляется из попапа ("ALL" = удаление всех). */
    val calendarLangDeleting: String? = null,
    val calendarLoading: Boolean = false,
    val backfillLoading: Boolean = false,
    val backfillResult: String? = null,
    // ── Billing ───────────────────────────────────────────────────────────────
    val billingLoading: Boolean = false,
    val billingInfo: AnthropicUsageInfo? = null,
    val billingError: String? = null,
)

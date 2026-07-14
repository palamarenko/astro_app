package com.iruna.app.ui.screens.admin

import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

// ── Calendar ──────────────────────────────────────────────────────────────────

fun AdminViewModel.setCalendarPeriod(period: HoroscopePeriod) {
    _state.value = _state.value.copy(calendarPeriod = period, calendarMeta = emptyMap())
    loadCalendarData()
}

fun AdminViewModel.navigateCalendarMonth(forward: Boolean) {
    val st = _state.value
    _state.value = if (forward) {
        if (st.calendarViewMonth == 12) st.copy(calendarViewMonth = 1, calendarViewYear = st.calendarViewYear + 1)
        else st.copy(calendarViewMonth = st.calendarViewMonth + 1)
    } else {
        if (st.calendarViewMonth == 1) st.copy(calendarViewMonth = 12, calendarViewYear = st.calendarViewYear - 1)
        else st.copy(calendarViewMonth = st.calendarViewMonth - 1)
    }
}

fun AdminViewModel.navigateCalendarYear(forward: Boolean) {
    val st = _state.value
    _state.value = st.copy(calendarViewYear = if (forward) st.calendarViewYear + 1 else st.calendarViewYear - 1)
}

/** Грузит мету по каждому языку и возвращает (суммарная_мета, мета_по_языкам). */
private suspend fun AdminViewModel.fetchCalendarMeta(): Pair<Map<String, Int>, Map<String, Map<String, Int>>> =
    coroutineScope {
        val period = _state.value.calendarPeriod.id
        // lang → (dateKey → count)
        val metaByLang: Map<String, Map<String, Int>> = ALL_GEN_LANGS
            .map { lang -> async { lang to firebase.getHoroscopeMeta(lang, period) } }
            .awaitAll()
            .toMap()
        // Суммарно по всем языкам: max = 12 знаков × число языков
        val allKeys = metaByLang.values.flatMap { it.keys }.toSet()
        val merged  = allKeys.associateWith { key -> metaByLang.values.sumOf { it[key] ?: 0 } }
        merged to metaByLang
    }

fun AdminViewModel.loadCalendarData() {
    viewModelScope.launch {
        _state.value = _state.value.copy(calendarLoading = true)
        val (merged, byLang) = fetchCalendarMeta()
        _state.value = _state.value.copy(
            calendarMeta = merged,
            calendarMetaByLang = byLang,
            calendarLoading = false,
        )
    }
}

fun AdminViewModel.openCalendarPopup(dateKey: String) {
    _state.value = _state.value.copy(calendarPopupKey = dateKey)
}

fun AdminViewModel.closeCalendarPopup() {
    _state.value = _state.value.copy(calendarPopupKey = null)
}

/** Удаляет один язык для указанного dateKey и обновляет мету. */
fun AdminViewModel.deleteCalendarLang(dateKey: String, lang: String) {
    if (_state.value.calendarLangDeleting != null) return
    viewModelScope.launch {
        _state.value = _state.value.copy(calendarLangDeleting = lang)
        firebase.deleteLangDateKey(lang, _state.value.calendarPeriod.id, dateKey)
        val (merged, byLang) = fetchCalendarMeta()
        _state.value = _state.value.copy(
            calendarMeta = merged,
            calendarMetaByLang = byLang,
            calendarLangDeleting = null,
        )
    }
}

fun AdminViewModel.setDateAbsolute(date: LocalDate, period: HoroscopePeriod) {
    _state.value = _state.value.copy(
        selectedDate = date,
        period       = period,
        isLoaded     = false,
        savedCount   = -1,
        saveError    = null,
    )
    load()
}

fun AdminViewModel.deletePeriod(dateKey: String) {
    if (_state.value.calendarLangDeleting != null) return
    viewModelScope.launch {
        _state.value = _state.value.copy(calendarLangDeleting = "ALL")
        firebase.deleteAllLangsDateKey(_state.value.calendarPeriod.id, dateKey)
        firebase.deleteHoroscopeMeta(_state.value.calendarPeriod.id, dateKey)
        val (merged, byLang) = fetchCalendarMeta()
        _state.value = _state.value.copy(
            calendarMeta = merged,
            calendarMetaByLang = byLang,
            calendarLangDeleting = null,
            calendarPopupKey = null,
        )
    }
}

fun AdminViewModel.backfillMeta() {
    if (_state.value.backfillLoading) return
    viewModelScope.launch {
        _state.value = _state.value.copy(backfillLoading = true, backfillResult = null)
        val period = _state.value.calendarPeriod.id
        var saved  = 0
        try {
            listOf("ru", "uk", "en", "es", "de", "fr").forEach { lang ->
                val dateKeys = firebase.getAvailableDateKeys(lang, period)
                val jobs = dateKeys.map { dateKey ->
                    async {
                        val count = firebase.getSignCountForDateKey(lang, period, dateKey)
                        firebase.saveHoroscopeMeta(lang, period, dateKey, count)
                    }
                }
                jobs.awaitAll()
                saved += dateKeys.size
            }
            _state.value = _state.value.copy(backfillLoading = false, backfillResult = "✓ Backfilled $saved entries")
        } catch (e: Exception) {
            _state.value = _state.value.copy(backfillLoading = false, backfillResult = "⚠ ${e.message ?: "Error"}")
        }
        loadCalendarData()
    }
}

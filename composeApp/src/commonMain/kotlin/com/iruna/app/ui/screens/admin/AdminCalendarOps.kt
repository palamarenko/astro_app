package com.iruna.app.ui.screens.admin

import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

fun AdminViewModel.loadCalendarData() {
    viewModelScope.launch {
        _state.value = _state.value.copy(calendarLoading = true)
        val period = _state.value.calendarPeriod.id
        val allLangs = listOf("ru", "uk", "en", "es", "de", "fr")
        val metaByLang = allLangs
            .map { lang -> async { firebase.getHoroscopeMeta(lang, period) } }
            .map { it.await() }
        // Суммируем по всем 6 языкам: max = 72 (12 знаков × 6 языков)
        val allKeys = metaByLang.flatMap { it.keys }.toSet()
        val merged  = allKeys.associateWith { key ->
            metaByLang.sumOf { it[key] ?: 0 }
        }
        _state.value = _state.value.copy(calendarMeta = merged, calendarLoading = false)
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
    viewModelScope.launch {
        _state.value = _state.value.copy(calendarLoading = true)
        firebase.deleteAllLangsDateKey(_state.value.calendarPeriod.id, dateKey)
        firebase.deleteHoroscopeMeta(_state.value.calendarPeriod.id, dateKey)
        loadCalendarData()
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

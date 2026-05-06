package com.astro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private fun computeDateKey(period: HoroscopePeriod, date: LocalDate): String = when (period) {
    HoroscopePeriod.DAILY   -> date.toString()
    HoroscopePeriod.WEEKLY  -> {
        val week = (date.dayOfYear / 7) + 1
        "${date.year}-W${week.toString().padStart(2, '0')}"
    }
    HoroscopePeriod.MONTHLY -> "${date.year}-${date.monthNumber.toString().padStart(2, '0')}"
}

private fun defaultHoroscopes(): Map<String, HoroscopeResponse> =
    ALL_SIGNS.associate { it.id to HoroscopeResponse("", 75, 75, 75, 75) }

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
)

class AdminViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(period = period, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun navigateDate(forward: Boolean) {
        val d = _state.value.selectedDate
        val newDate: LocalDate = when (_state.value.period) {
            HoroscopePeriod.DAILY   -> if (forward) d.plus(1, DateTimeUnit.DAY)  else d.plus(-1, DateTimeUnit.DAY)
            HoroscopePeriod.WEEKLY  -> if (forward) d.plus(7, DateTimeUnit.DAY)  else d.plus(-7, DateTimeUnit.DAY)
            HoroscopePeriod.MONTHLY -> {
                val m = d.monthNumber; val y = d.year
                if (forward) {
                    if (m == 12) LocalDate(y + 1, 1, 1) else LocalDate(y, m + 1, 1)
                } else {
                    if (m == 1)  LocalDate(y - 1, 12, 1) else LocalDate(y, m - 1, 1)
                }
            }
        }
        _state.value = _state.value.copy(selectedDate = newDate, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadError = null, savedCount = -1, saveError = null)
            try {
                val key = computeDateKey(_state.value.period, _state.value.selectedDate)
                val loaded = firebase.getAllSignHoroscopes(_state.value.lang, _state.value.period.id, key)
                val merged = ALL_SIGNS.associate { sign ->
                    sign.id to (loaded?.get(sign.id) ?: HoroscopeResponse("", 75, 75, 75, 75))
                }
                _state.value = _state.value.copy(horoscopes = merged, isLoading = false, isLoaded = true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                _state.value = _state.value.copy(isLoading = false, loadError = e.message ?: "Load error")
            }
        }
    }

    fun updateText(signId: String, text: String) {
        val h = _state.value.horoscopes.toMutableMap()
        h[signId] = (h[signId] ?: HoroscopeResponse("", 75, 75, 75, 75)).copy(text = text)
        _state.value = _state.value.copy(horoscopes = h)
    }

    fun updateScore(signId: String, field: String, delta: Int) {
        val h = _state.value.horoscopes.toMutableMap()
        val cur = h[signId] ?: HoroscopeResponse("", 75, 75, 75, 75)
        h[signId] = when (field) {
            "love"   -> cur.copy(love   = (cur.love   + delta).coerceIn(50, 100))
            "career" -> cur.copy(career = (cur.career + delta).coerceIn(50, 100))
            "health" -> cur.copy(health = (cur.health + delta).coerceIn(50, 100))
            "energy" -> cur.copy(energy = (cur.energy + delta).coerceIn(50, 100))
            else -> cur
        }
        _state.value = _state.value.copy(horoscopes = h)
    }

    fun generateForSign(sign: ZodiacSign) {
        if (_state.value.generatingSignIds.contains(sign.id)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(generatingSignIds = _state.value.generatingSignIds + sign.id)
            try {
                val st = _state.value
                val key = computeDateKey(st.period, st.selectedDate)
                val response = api.generateAdminHoroscope(sign, st.period, st.lang, key)
                val h = _state.value.horoscopes.toMutableMap()
                h[sign.id] = response
                _state.value = _state.value.copy(
                    horoscopes = h,
                    generatingSignIds = _state.value.generatingSignIds - sign.id
                )
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException)
                    _state.value = _state.value.copy(generatingSignIds = _state.value.generatingSignIds - sign.id)
            }
        }
    }

    fun generateAllSigns() {
        if (_state.value.isGeneratingAll) return
        viewModelScope.launch {
            val st = _state.value
            val key = computeDateKey(st.period, st.selectedDate)
            val allIds = ALL_SIGNS.map { it.id }.toSet()
            _state.value = _state.value.copy(isGeneratingAll = true, generatingSignIds = allIds)
            try {
                val deferreds = ALL_SIGNS.map { sign ->
                    async {
                        try {
                            val response = api.generateAdminHoroscope(sign, st.period, st.lang, key)
                            val h = _state.value.horoscopes.toMutableMap()
                            h[sign.id] = response
                            _state.value = _state.value.copy(
                                horoscopes = h,
                                generatingSignIds = _state.value.generatingSignIds - sign.id
                            )
                        } catch (e: Exception) {
                            if (e !is kotlinx.coroutines.CancellationException)
                                _state.value = _state.value.copy(generatingSignIds = _state.value.generatingSignIds - sign.id)
                        }
                    }
                }
                deferreds.awaitAll()
            } finally {
                _state.value = _state.value.copy(isGeneratingAll = false, generatingSignIds = emptySet())
            }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, savedCount = -1, saveError = null)
            try {
                val key = computeDateKey(_state.value.period, _state.value.selectedDate)
                var count = 0
                _state.value.horoscopes.forEach { (signId, horoscope) ->
                    if (horoscope.text.isNotBlank()) {
                        firebase.saveFullHoroscope(_state.value.lang, _state.value.period.id, key, signId, horoscope)
                        count++
                    }
                }
                _state.value = _state.value.copy(isSaving = false, savedCount = count)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, saveError = e.message ?: "Save error")
            }
        }
    }
}

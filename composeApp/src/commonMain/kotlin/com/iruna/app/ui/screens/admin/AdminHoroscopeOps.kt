package com.iruna.app.ui.screens.admin

import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

// ── Horoscope load / edit / generate / save ───────────────────────────────────

fun AdminViewModel.load() {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, loadError = null, savedCount = -1, saveError = null)
        try {
            val key = computeDateKey(_state.value.period, _state.value.selectedDate)
            val loaded = firebase.getAllSignHoroscopes(_state.value.lang, _state.value.period.id, key)
            val merged = ALL_SIGNS.associate { sign ->
                sign.id to (loaded?.get(sign.id) ?: HoroscopeResponse(text = "", love = 75, career = 75, health = 75, energy = 75))
            }
            _state.value = _state.value.copy(horoscopes = merged, isLoading = false, isLoaded = true)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) return@launch
            _state.value = _state.value.copy(isLoading = false, loadError = e.message ?: "Load error")
        }
    }
}

fun AdminViewModel.updateText(signId: String, text: String) {
    val h = _state.value.horoscopes.toMutableMap()
    h[signId] = (h[signId] ?: HoroscopeResponse(text = "", love = 75, career = 75, health = 75, energy = 75)).copy(text = text)
    _state.value = _state.value.copy(horoscopes = h)
}

fun AdminViewModel.updateScore(signId: String, field: String, delta: Int) {
    val h = _state.value.horoscopes.toMutableMap()
    val cur = h[signId] ?: HoroscopeResponse(text = "", love = 75, career = 75, health = 75, energy = 75)
    h[signId] = when (field) {
        "love"   -> cur.copy(love   = (cur.love   + delta).coerceIn(50, 100))
        "career" -> cur.copy(career = (cur.career + delta).coerceIn(50, 100))
        "health" -> cur.copy(health = (cur.health + delta).coerceIn(50, 100))
        "energy" -> cur.copy(energy = (cur.energy + delta).coerceIn(50, 100))
        else -> cur
    }
    _state.value = _state.value.copy(horoscopes = h)
}

fun AdminViewModel.generateForSign(sign: ZodiacSign) {
    if (_state.value.generatingSignIds.contains(sign.id)) return
    viewModelScope.launch {
        _state.value = _state.value.copy(generatingSignIds = _state.value.generatingSignIds + sign.id, generateError = null)
        try {
            val st  = _state.value
            val key = computeDateKey(st.period, st.selectedDate)
            val response = api.generateAdminHoroscope(sign, st.period, st.lang, key, st.promptText.takeIf { it.isNotBlank() })
            val h = _state.value.horoscopes.toMutableMap()
            h[sign.id] = response
            _state.value = _state.value.copy(horoscopes = h, generatingSignIds = _state.value.generatingSignIds - sign.id)
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException)
                _state.value = _state.value.copy(
                    generatingSignIds = _state.value.generatingSignIds - sign.id,
                    generateError = e.message ?: "Generation error",
                )
        }
    }
}

fun AdminViewModel.generateAllSigns() {
    if (_state.value.isGeneratingAll) return
    viewModelScope.launch {
        val st  = _state.value
        val key = computeDateKey(st.period, st.selectedDate)
        _state.value = _state.value.copy(isGeneratingAll = true, generatingSignIds = ALL_SIGNS.map { it.id }.toSet(), generateError = null)
        try {
            val maxAttempts = 3
            var remaining   = ALL_SIGNS.toList()
            repeat(maxAttempts) { attempt ->
                if (remaining.isEmpty()) return@repeat
                try {
                    // Один запрос — все оставшиеся знаки сразу
                    val results = api.generateAdminAllSigns(
                        signs             = remaining,
                        period            = st.period,
                        lang              = st.lang,
                        dateKey           = key,
                        styleInstructions = st.promptText.takeIf { it.isNotBlank() },
                    )
                    val h = _state.value.horoscopes.toMutableMap()
                    results.forEach { (signId, response) ->
                        h[signId] = response
                    }
                    val doneIds = results.keys
                    _state.value = _state.value.copy(
                        horoscopes        = h,
                        generatingSignIds = _state.value.generatingSignIds - doneIds,
                    )
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // При ошибке всего запроса — сохраняем сообщение, попробуем ещё раз
                    _state.value = _state.value.copy(generateError = e.message ?: "Generation error")
                }
                remaining = ALL_SIGNS.filter { sign -> _state.value.horoscopes[sign.id]?.text.isNullOrBlank() }
                if (remaining.isNotEmpty() && attempt < maxAttempts - 1)
                    _state.value = _state.value.copy(generatingSignIds = remaining.map { it.id }.toSet())
            }
        } finally {
            _state.value = _state.value.copy(isGeneratingAll = false, generatingSignIds = emptySet())
        }
    }
}

fun AdminViewModel.saveAll() {
    viewModelScope.launch {
        _state.value = _state.value.copy(isSaving = true, savedCount = -1, saveError = null)
        try {
            val key   = computeDateKey(_state.value.period, _state.value.selectedDate)
            var count = 0
            _state.value.horoscopes.forEach { (signId, horoscope) ->
                if (horoscope.text.isNotBlank()) {
                    firebase.saveFullHoroscope(_state.value.lang, _state.value.period.id, key, signId, horoscope)
                    count++
                }
            }
            firebase.saveHoroscopeMeta(_state.value.lang, _state.value.period.id, key, count)
            _state.value = _state.value.copy(isSaving = false, savedCount = count)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isSaving = false, saveError = e.message ?: "Save error")
        }
    }
}

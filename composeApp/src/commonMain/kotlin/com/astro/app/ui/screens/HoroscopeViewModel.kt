package com.astro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class HoroscopeUiState(
    val selectedSign: ZodiacSign? = null,
    val period: HoroscopePeriod = HoroscopePeriod.DAILY,
    val horoscope: HoroscopeResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HoroscopeViewModel(
    private val firebase: FirebaseService = FirebaseService()
) : ViewModel() {
    private val _state = MutableStateFlow(HoroscopeUiState())
    val state: StateFlow<HoroscopeUiState> = _state.asStateFlow()

    fun selectSign(sign: ZodiacSign) {
        _state.value = _state.value.copy(selectedSign = sign)
        loadHoroscope(sign, _state.value.period)
    }

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(period = period)
        _state.value.selectedSign?.let { loadHoroscope(it, period) }
    }

    private fun loadHoroscope(sign: ZodiacSign, period: HoroscopePeriod) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, horoscope = null, error = null)
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val date = when (period) {
                    HoroscopePeriod.DAILY   -> today.toString()
                    HoroscopePeriod.WEEKLY  -> {
                        val week = (today.dayOfYear / 7) + 1
                        "${today.year}-W${week.toString().padStart(2, '0')}"
                    }
                    HoroscopePeriod.MONTHLY -> "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
                }
                val response = firebase.getHoroscope("ru", period.id, date, sign.id)
                _state.value = _state.value.copy(horoscope = response, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Unknown error", isLoading = false)
            }
        }
    }
}

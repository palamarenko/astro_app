package com.astro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val sign: ZodiacSign = ALL_SIGNS[4],
    val name: String = "",
    val gender: String = "",        // "male" | "female" | ""
    val birthDay: Int = 0,
    val birthMonth: Int = 0,
    val birthYear: Int = 1990,
    val birthHour: Int = -1,
    val birthMinute: Int = 0,
    val birthPlace: String = "",
    val birthLat: Double = 0.0,
    val birthLng: Double = 0.0,
    val isLoading: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showPlacePicker: Boolean = false,
    // ── Onboarding ────────────────────────────────────────────────────────────
    val onboardingStep: Int = 0,
    val onboardingFinished: Boolean = false,
)

private fun zodiacFromDate(month: Int, day: Int): ZodiacSign = when {
    (month == 3  && day >= 21) || (month == 4  && day <= 19) -> ALL_SIGNS.first { it.id == "aries" }
    (month == 4  && day >= 20) || (month == 5  && day <= 20) -> ALL_SIGNS.first { it.id == "taurus" }
    (month == 5  && day >= 21) || (month == 6  && day <= 20) -> ALL_SIGNS.first { it.id == "gemini" }
    (month == 6  && day >= 21) || (month == 7  && day <= 22) -> ALL_SIGNS.first { it.id == "cancer" }
    (month == 7  && day >= 23) || (month == 8  && day <= 22) -> ALL_SIGNS.first { it.id == "leo" }
    (month == 8  && day >= 23) || (month == 9  && day <= 22) -> ALL_SIGNS.first { it.id == "virgo" }
    (month == 9  && day >= 23) || (month == 10 && day <= 22) -> ALL_SIGNS.first { it.id == "libra" }
    (month == 10 && day >= 23) || (month == 11 && day <= 21) -> ALL_SIGNS.first { it.id == "scorpio" }
    (month == 11 && day >= 22) || (month == 12 && day <= 21) -> ALL_SIGNS.first { it.id == "sagittarius" }
    (month == 12 && day >= 22) || (month == 1  && day <= 19) -> ALL_SIGNS.first { it.id == "capricorn" }
    (month == 1  && day >= 20) || (month == 2  && day <= 18) -> ALL_SIGNS.first { it.id == "aquarius" }
    else                                                       -> ALL_SIGNS.first { it.id == "pisces" }
}

private fun UserProfile.toUiState(): ProfileUiState {
    val sign = ALL_SIGNS.firstOrNull { it.id == signId } ?: ALL_SIGNS[4]
    return ProfileUiState(
        sign = sign,
        name = name,
        gender = gender,
        birthDay = birthDay,
        birthMonth = birthMonth,
        birthYear = birthYear,
        birthHour = birthHour,
        birthMinute = birthMinute,
        birthPlace = birthPlace,
        birthLat = birthLat,
        birthLng = birthLng,
        onboardingStep = onboardingStep,
        onboardingFinished = onboardingFinished,
    )
}

private fun ProfileUiState.toProfile() = UserProfile(
    signId = sign.id,
    name = name,
    gender = gender,
    birthDay = birthDay,
    birthMonth = birthMonth,
    birthYear = birthYear,
    birthHour = birthHour,
    birthMinute = birthMinute,
    birthPlace = birthPlace,
    birthLat = birthLat,
    birthLng = birthLng,
    onboardingStep = onboardingStep,
    onboardingFinished = onboardingFinished,
)

class ProfileViewModel(private val api: ClaudeApiClient) : ViewModel() {

    private val _state = MutableStateFlow(
        UserStorage.load()?.toUiState() ?: ProfileUiState()
    )
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private fun update(newState: ProfileUiState) {
        _state.value = newState
        UserStorage.save(newState.toProfile())
    }

    fun setName(name: String) {
        update(_state.value.copy(name = name))
    }

    fun setGender(gender: String) {
        update(_state.value.copy(gender = gender))
    }

    fun selectSign(sign: ZodiacSign) {
        update(_state.value.copy(sign = sign))
    }

    fun setBirthDate(day: Int, month: Int, year: Int) {
        update(_state.value.copy(
            birthDay = day, birthMonth = month, birthYear = year,
            sign = zodiacFromDate(month, day),
            showDatePicker = false
        ))
    }

    fun setBirthTime(hour: Int, minute: Int) {
        update(_state.value.copy(birthHour = hour, birthMinute = minute, showTimePicker = false))
    }

    fun setPlace(name: String, lat: Double = 0.0, lng: Double = 0.0) {
        update(_state.value.copy(birthPlace = name, birthLat = lat, birthLng = lng, showPlacePicker = false))
    }

    fun showDatePicker()  { _state.value = _state.value.copy(showDatePicker  = true) }
    fun hideDatePicker()  { _state.value = _state.value.copy(showDatePicker  = false) }
    fun showTimePicker()  { _state.value = _state.value.copy(showTimePicker  = true) }
    fun hideTimePicker()  { _state.value = _state.value.copy(showTimePicker  = false) }
    fun showPlacePicker() { _state.value = _state.value.copy(showPlacePicker = true) }
    fun hidePlacePicker() { _state.value = _state.value.copy(showPlacePicker = false) }

    // ── Onboarding state management ───────────────────────────────────────────
    /** Сохранить, на каком шаге онбординга остановился пользователь (для возобновления). */
    fun setOnboardingStep(step: Int) {
        update(_state.value.copy(onboardingStep = step))
    }

    /** Полное завершение/пропуск онбординга — больше не показываем. */
    fun finishOnboarding() {
        update(_state.value.copy(onboardingFinished = true))
    }

    // Compat — больше не используется, оставлено чтобы не сломать App.kt
    fun toggleSignPicker() {}
}

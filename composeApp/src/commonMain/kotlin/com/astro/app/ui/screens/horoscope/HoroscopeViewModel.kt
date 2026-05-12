package com.astro.app.ui.screens.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import com.astro.app.i18n.AppLanguage
import com.astro.app.i18n.LanguageManager
import com.astro.app.i18n.str
import com.astro.app.notifications.subscribeToPushTopic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*

// ── UI State ──────────────────────────────────────────────────────────────────

data class HoroscopeUiState(
    val selectedSign: ZodiacSign? = null,
    val period: HoroscopePeriod = HoroscopePeriod.DAILY,
    // Per-tab forecast caches (null = not yet loaded, loading in progress assumed)
    val current:  Map<HoroscopePeriod, HoroscopeResponse?> = emptyMap(),
    val future:   Map<HoroscopePeriod, HoroscopeResponse?> = emptyMap(),
    // Which tabs have the future unlocked via wizard
    val unlocked: Map<HoroscopePeriod, Boolean> = HoroscopePeriod.entries.associateWith { false },
    val loadingCurrent: Boolean = false,
    val loadingFuture:  Boolean = false,
    val error: String? = null,
    val showWizard: Boolean = false,
    // Push notifications prompt — показываем один раз при первом заходе
    val showPushPrompt: Boolean = false,
) {
    val currentForecast: HoroscopeResponse? get() = current[period]
    val futureForecast:  HoroscopeResponse? get() = future[period]
    val isUnlocked:      Boolean             get() = unlocked[period] ?: false
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class HoroscopeViewModel(
    private val firebase: FirebaseService = FirebaseService(),
) : ViewModel() {

    private val _state = MutableStateFlow(HoroscopeUiState())
    val state: StateFlow<HoroscopeUiState> = _state.asStateFlow()

    private val lang: String
        get() = when (LanguageManager.current) {
            AppLanguage.UK -> "uk"
            AppLanguage.EN -> "en"
            else           -> "ru"
        }

    init {
        // Проверяем нужно ли показать push-промпт при первом заходе
        val profile = UserStorage.load()
        if (profile != null && !profile.pushNotificationsAsked) {
            _state.value = _state.value.copy(showPushPrompt = true)
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun selectSign(sign: ZodiacSign) {
        // Reset all caches when sign changes, keep push prompt state
        val pushPrompt = _state.value.showPushPrompt
        _state.value = HoroscopeUiState(selectedSign = sign, showPushPrompt = pushPrompt)
        loadCurrentIfNeeded(sign, HoroscopePeriod.DAILY)
    }

    /** Вызывается после того, как пользователь ответил на push-промпт. */
    fun onPushPromptResult(enabled: Boolean) {
        val profile = UserStorage.load() ?: UserProfile()
        UserStorage.save(profile.copy(
            pushNotificationsAsked   = true,
            pushNotificationsEnabled = enabled,
        ))
        _state.value = _state.value.copy(showPushPrompt = false)
        // Подписываемся на FCM-топик, чтобы можно было слать всем сразу
        if (enabled) subscribeToFcmTopic()
    }

    /** expect/actual — на Android вызывает FirebaseMessaging.subscribeToTopic() */
    private fun subscribeToFcmTopic() = subscribeToPushTopic()

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(period = period)
        _state.value.selectedSign?.let { loadCurrentIfNeeded(it, period) }
    }

    fun showWizard() {
        _state.value = _state.value.copy(showWizard = true)
    }

    fun dismissWizard() {
        _state.value = _state.value.copy(showWizard = false)
    }

    fun unlockAndLoadFuture(period: HoroscopePeriod) {
       viewModelScope.launch {
           delay(5000)
           val newUnlocked = _state.value.unlocked.toMutableMap().also { it[period] = true }
           _state.value = _state.value.copy(
               showWizard = false,
               unlocked   = newUnlocked,
           )
           _state.value.selectedSign?.let { loadFutureIfNeeded(it, period) }
       }
    }

    // ── Private loaders ───────────────────────────────────────────────────────

    private fun loadCurrentIfNeeded(sign: ZodiacSign, period: HoroscopePeriod) {
        if (_state.value.current.containsKey(period)) return   // already cached
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingCurrent = true, error = null)
            val result = fetchHoroscope(sign, period, future = false)
            val newCurrent = _state.value.current.toMutableMap().also { it[period] = result }
            _state.value = _state.value.copy(current = newCurrent, loadingCurrent = false,
                error = if (result == null) "Данные не найдены" else null)
        }
    }

    private fun loadFutureIfNeeded(sign: ZodiacSign, period: HoroscopePeriod) {
        if (_state.value.future.containsKey(period)) return    // already cached
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingFuture = true)
            val result = fetchHoroscope(sign, period, future = true)
            val newFuture = _state.value.future.toMutableMap().also { it[period] = result }
            _state.value = _state.value.copy(future = newFuture, loadingFuture = false)
        }
    }

    private suspend fun fetchHoroscope(
        sign: ZodiacSign,
        period: HoroscopePeriod,
        future: Boolean,
    ): HoroscopeResponse? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dateKey = if (future) futureDateKey(period, today) else currentDateKey(period, today)
        return try {
            firebase.getHoroscope(lang, period.id, dateKey, sign.id)
        } catch (_: Exception) {
            null
        }
    }

    // ── Date key helpers ──────────────────────────────────────────────────────

    private fun currentDateKey(period: HoroscopePeriod, today: LocalDate): String = when (period) {
        HoroscopePeriod.DAILY   -> today.toString()
        HoroscopePeriod.WEEKLY  -> {
            val week = (today.dayOfYear / 7) + 1
            "${today.year}-W${week.toString().padStart(2, '0')}"
        }
        HoroscopePeriod.MONTHLY -> "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
    }

    private fun futureDateKey(period: HoroscopePeriod, today: LocalDate): String = when (period) {
        HoroscopePeriod.DAILY   -> {
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            tomorrow.toString()
        }
        HoroscopePeriod.WEEKLY  -> {
            val nextWeek = today.plus(7, DateTimeUnit.DAY)
            val week = (nextWeek.dayOfYear / 7) + 1
            "${nextWeek.year}-W${week.toString().padStart(2, '0')}"
        }
        HoroscopePeriod.MONTHLY -> {
            val nextMonth = today.plus(1, DateTimeUnit.MONTH)
            "${nextMonth.year}-${nextMonth.monthNumber.toString().padStart(2, '0')}"
        }
    }

}

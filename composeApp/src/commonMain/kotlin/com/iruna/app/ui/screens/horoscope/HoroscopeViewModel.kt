package com.iruna.app.ui.screens.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import com.iruna.app.i18n.AppLanguage
import com.iruna.app.i18n.LanguageManager
import com.iruna.app.i18n.str
import com.iruna.app.notifications.subscribeToPushTopic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
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
    // ── «Карта дня» ────────────────────────────────────────────────────────────
    val showDayCard: Boolean = false,
    val dayCard: TarotCard? = null,       // выбранная по дате карта
    val dayCardText: String? = null,      // прогноз (null = ещё грузится / отсутствует)
    val dayCardLoading: Boolean = false,
    val dayCardReveal: Boolean = false,   // true = проиграть анимацию раскрытия (флип) — только при первом открытии за день
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
            AppLanguage.RU -> "ru"
            AppLanguage.UK -> "uk"
            AppLanguage.EN -> "en"
            AppLanguage.ES -> "es"
            AppLanguage.DE -> "de"
            AppLanguage.FR -> "fr"
            AppLanguage.AR -> "ar"
        }

    init {
        // init запускается при создании VM (до завершения онбординга).
        // Показываем промпт только если онбординг уже пройден И разрешение ещё не запрашивали.
        // Для новых пользователей (profile == null) промпт покажет checkPushPrompt(),
        // которая вызывается из App.kt после завершения онбординга.
        val profile = UserStorage.load()
        if (profile != null && profile.onboardingFinished && !profile.pushNotificationsAsked) {
            _state.value = _state.value.copy(showPushPrompt = true)
        }
    }

    /**
     * Вызывается из App.kt сразу после того, как онбординг завершился.
     * Показывает промпт, если разрешение ещё ни разу не запрашивалось.
     */
    fun checkPushPrompt() {
        val profile = UserStorage.load() ?: return
        if (!profile.pushNotificationsAsked) {
            Track.pushPromptShown()
            _state.value = _state.value.copy(showPushPrompt = true)
        }
    }

    /**
     * Пользователь нажал «Позже» — скрываем промпт только на эту сессию.
     * НЕ сохраняем pushNotificationsAsked = true, чтобы в следующей сессии
     * промпт снова появился.
     */
    fun dismissPushPromptForSession() {
        Track.pushPromptDismiss()
        _state.value = _state.value.copy(showPushPrompt = false)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun selectSign(sign: ZodiacSign) {
        Track.horoscopeSignSelect(sign.id)
        // Reset all caches when sign changes, keep push prompt state
        val pushPrompt = _state.value.showPushPrompt
        _state.value = HoroscopeUiState(selectedSign = sign, showPushPrompt = pushPrompt)
        loadCurrentIfNeeded(sign, HoroscopePeriod.DAILY)
    }

    /** Вызывается после того, как пользователь ответил на push-промпт. */
    fun onPushPromptResult(enabled: Boolean) {
        Track.pushPermissionResult(enabled)
        Analytics.setUserProperty(AnalyticsUserProp.NOTIFICATIONS_ENABLED, enabled.toString())
        val profile = UserStorage.load() ?: UserProfile()
        UserStorage.save(profile.copy(
            pushNotificationsAsked   = true,
            pushNotificationsEnabled = enabled,
        ))
        _state.value = _state.value.copy(showPushPrompt = false)
        // Подписываемся на FCM-топик, чтобы можно было слать всем сразу
        if (enabled) {
            Track.pushTopicSubscribe("all")
            subscribeToFcmTopic()
        }
    }

    /** expect/actual — на Android вызывает FirebaseMessaging.subscribeToTopic() */
    private fun subscribeToFcmTopic() = subscribeToPushTopic()

    fun setPeriod(period: HoroscopePeriod) {
        Track.horoscopePeriodSelect(period.id)
        _state.value = _state.value.copy(period = period)
        _state.value.selectedSign?.let { loadCurrentIfNeeded(it, period) }
    }

    // ── «Карта дня» ─────────────────────────────────────────────────────────────

    /** Кеш текста «Карты дня» в рамках сессии: ключ "lang:cardKey" → текст (null = прогноза нет).
     *  Позволяет при повторном открытии показать текст сразу, без запроса к серверу. */
    private val dayCardTextCache = mutableMapOf<String, String?>()

    /** Открывает попап «Карты дня». Карта детерминирована датой (одна на день
     *  для всех пользователей), текст-прогноз подгружается из Firebase и кешируется
     *  на сессию. */
    fun openDayCard() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val idx = today.toEpochDays().toInt().mod(ALL_TAROT.size)
        val card = ALL_TAROT[idx]
        Track.screen("day_card")

        // Анимацию раскрытия (флип рубашкой вверх) показываем только при первом
        // открытии карты в этот день. Дальше — карта сразу лицом вверх.
        val todayKey = TarotStorage.todayKey()
        val reveal = TarotStorage.dayCardRevealedDate() != todayKey
        if (reveal) TarotStorage.setDayCardRevealedDate(todayKey)

        // Кеш-хит — показываем текст сразу, без обращения к серверу.
        val cacheKey = "$lang:${card.resourceKey}"
        if (dayCardTextCache.containsKey(cacheKey)) {
            _state.value = _state.value.copy(
                showDayCard = true,
                dayCard = card,
                dayCardText = dayCardTextCache[cacheKey],
                dayCardLoading = false,
                dayCardReveal = reveal,
            )
            return
        }

        _state.value = _state.value.copy(
            showDayCard = true,
            dayCard = card,
            dayCardText = null,
            dayCardLoading = true,
            dayCardReveal = reveal,
        )
        viewModelScope.launch {
            val text = try {
                val cards = firebase.getAllDayCards(lang)
                var t = cards?.get(card.resourceKey)?.text
                // Фолбэк на английский, если на текущем языке текста нет
                if (t.isNullOrBlank() && lang != "en") {
                    t = firebase.getAllDayCards("en")?.get(card.resourceKey)?.text
                }
                t
            } catch (_: Exception) {
                null
            }
            val resolved = text?.takeIf { it.isNotBlank() }
            dayCardTextCache[cacheKey] = resolved
            // Пользователь мог закрыть попап пока грузилось — не перетираем состояние
            if (_state.value.showDayCard && _state.value.dayCard?.resourceKey == card.resourceKey) {
                _state.value = _state.value.copy(
                    dayCardText = resolved,
                    dayCardLoading = false,
                )
            }
        }
    }

    fun dismissDayCard() {
        _state.value = _state.value.copy(showDayCard = false)
    }

    fun showWizard() {
        _state.value.selectedSign?.let { Track.horoscopeWizardCtaClick(it.id, _state.value.period.id) }
        _state.value = _state.value.copy(showWizard = true)
    }

    fun dismissWizard() {
        _state.value = _state.value.copy(showWizard = false)
    }

    fun unlockAndLoadFuture(period: HoroscopePeriod) {
       _state.value.selectedSign?.let { Track.horoscopeWizardGenerated(it.id, period.id) }
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
            if (result != null) Track.horoscopeView(sign.id, period.id)
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
            val result = firebase.getHoroscope(lang, period.id, dateKey, sign.id)
            // Fallback to English if the horoscope is not available in the current language
            if (result == null && lang != "en") {
                firebase.getHoroscope("en", period.id, dateKey, sign.id)
            } else {
                result
            }
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

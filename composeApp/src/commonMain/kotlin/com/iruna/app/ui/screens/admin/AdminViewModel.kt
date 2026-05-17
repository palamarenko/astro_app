package com.iruna.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import com.iruna.app.data.UserStorage
import com.iruna.app.notifications.PushAdminService
import com.iruna.app.notifications.sendLocalTestPush
import io.ktor.client.plugins.HttpTimeout
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
    // ── Push notifications ────────────────────────────────────────────────────
    val functionUrl: String = "",
    val adminSecret: String = "",
    val pushSending: Boolean = false,
    val pushResult: String? = null,   // null = idle, "ok" = успех, иначе текст ошибки

    // ── Schedule ──────────────────────────────────────────────────────────────
    val scheduleHours: Set<Int> = emptySet(),   // local hours (for display)
    val scheduleLoading: Boolean = false,
    val scheduleSaving: Boolean = false,
    val scheduleSaved: Boolean = false,
    val scheduleError: String? = null,

    // ── Generate All Languages ────────────────────────────────────────────────
    val genAllLangsLoading: Boolean = false,
    val genAllLangsResult: String? = null,   // null = idle, "ok:X" = успех, иначе ошибка
    val genAllLangsPeriod: HoroscopePeriod = HoroscopePeriod.DAILY,
    val genAllLangsDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),

    // ── Horoscope Prompt ──────────────────────────────────────────────────────
    val promptText: String = "",
    val promptLoading: Boolean = false,
    val promptSaving: Boolean = false,
    val promptSaved: Boolean = false,
    val promptError: String? = null,

    // ── Generation schedule (авто-генерация гороскопов) ──────────────────────
    val genScheduleHours: Set<Int> = emptySet(),
    val genScheduleLoading: Boolean = false,
    val genScheduleSaving: Boolean = false,
    val genScheduleSaved: Boolean = false,
    val genScheduleError: String? = null,

    // ── Generation logs ───────────────────────────────────────────────────────
    val generationLogs: List<GenerationLogEntry> = emptyList(),
    val logsLoading: Boolean = false,
)

class AdminViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val firebase    = FirebaseService()
    private val pushService = PushAdminService(
        createHttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000L  // 5 min — enough for 36 Claude calls
                connectTimeoutMillis =  15_000L
            }
        }
    )
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        // Восстанавливаем сохранённые admin-настройки
        val saved = UserStorage.load()
        _state.value = _state.value.copy(
            functionUrl = saved?.adminFunctionUrl ?: "",
            adminSecret = saved?.adminSecret ?: "",
        )
    }

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(period = period, isLoaded = false, savedCount = -1, saveError = null,
            promptText = "", promptSaved = false, promptError = null)
        load()
        loadPrompt()
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

    fun loadAll() {
        load()
        loadPrompt()
        loadGenerationLogs()
    }

    fun load() {
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

    fun updateText(signId: String, text: String) {
        val h = _state.value.horoscopes.toMutableMap()
        h[signId] = (h[signId] ?: HoroscopeResponse(text = "", love = 75, career = 75, health = 75, energy = 75)).copy(text = text)
        _state.value = _state.value.copy(horoscopes = h)
    }

    fun updateScore(signId: String, field: String, delta: Int) {
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

    fun generateForSign(sign: ZodiacSign) {
        if (_state.value.generatingSignIds.contains(sign.id)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(generatingSignIds = _state.value.generatingSignIds + sign.id)
            try {
                val st = _state.value
                val key = computeDateKey(st.period, st.selectedDate)
                val response = api.generateAdminHoroscope(
                    sign, st.period, st.lang, key,
                    st.promptText.takeIf { it.isNotBlank() }
                )
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
            _state.value = _state.value.copy(
                isGeneratingAll = true,
                generatingSignIds = ALL_SIGNS.map { it.id }.toSet()
            )
            try {
                val maxAttempts = 3
                var remaining = ALL_SIGNS.toList()

                repeat(maxAttempts) { attempt ->
                    if (remaining.isEmpty()) return@repeat

                    val deferreds = remaining.map { sign ->
                        async {
                            try {
                                val response = api.generateAdminHoroscope(
                                    sign, st.period, st.lang, key,
                                    st.promptText.takeIf { it.isNotBlank() }
                                )
                                val h = _state.value.horoscopes.toMutableMap()
                                h[sign.id] = response
                                _state.value = _state.value.copy(
                                    horoscopes = h,
                                    generatingSignIds = _state.value.generatingSignIds - sign.id
                                )
                            } catch (e: Exception) {
                                if (e !is kotlinx.coroutines.CancellationException)
                                    _state.value = _state.value.copy(
                                        generatingSignIds = _state.value.generatingSignIds - sign.id
                                    )
                            }
                        }
                    }
                    deferreds.awaitAll()

                    // Проверяем кто не получил текст — они пойдут на следующую попытку
                    remaining = ALL_SIGNS.filter { sign ->
                        _state.value.horoscopes[sign.id]?.text.isNullOrBlank()
                    }

                    if (remaining.isNotEmpty() && attempt < maxAttempts - 1) {
                        // Возвращаем недогенерированных в индикатор загрузки
                        _state.value = _state.value.copy(
                            generatingSignIds = _state.value.generatingSignIds + remaining.map { it.id }.toSet()
                        )
                    }
                }
            } finally {
                _state.value = _state.value.copy(isGeneratingAll = false, generatingSignIds = emptySet())
            }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, savedCount = -1, saveError = null)
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

    // -- Push Notifications ---------------------------------------------------

    fun setFunctionUrl(url: String) {
        _state.value = _state.value.copy(functionUrl = url, pushResult = null)
        UserStorage.load()?.let { UserStorage.save(it.copy(adminFunctionUrl = url)) }
    }

    fun setAdminSecret(secret: String) {
        _state.value = _state.value.copy(adminSecret = secret, pushResult = null)
        UserStorage.load()?.let { UserStorage.save(it.copy(adminSecret = secret)) }
    }

    fun sendPushToSelf() {
        val profile  = UserStorage.load()
        val signId   = profile?.signId ?: "leo"
        val signName = ALL_SIGNS.find { it.id == signId }?.name ?: signId
        sendLocalTestPush(signName)
        _state.value = _state.value.copy(pushResult = "ok_self")
    }

    fun sendPushToAll() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) {
            _state.value = _state.value.copy(pushResult = "Enter Function URL and Admin Secret first")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(pushSending = true, pushResult = null)
            val result = pushService.sendToAll(functionUrl = url, adminSecret = secret)
            _state.value = _state.value.copy(
                pushSending = false,
                pushResult  = if (result.isSuccess) "ok_all" else (result.exceptionOrNull()?.message ?: "Error"),
            )
        }
    }

    fun clearPushResult() {
        _state.value = _state.value.copy(pushResult = null)
    }

    // -- Generate All Languages -----------------------------------------------

    fun setGenAllLangsPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(genAllLangsPeriod = period, genAllLangsResult = null)
    }

    fun navigateGenAllLangsDate(forward: Boolean) {
        val d = _state.value.genAllLangsDate
        val newDate: LocalDate = when (_state.value.genAllLangsPeriod) {
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
        _state.value = _state.value.copy(genAllLangsDate = newDate, genAllLangsResult = null)
    }

    fun generateAllLanguages() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) {
            _state.value = _state.value.copy(genAllLangsResult = "⚠ Enter Function URL and Admin Secret first")
            return
        }
        val period  = _state.value.genAllLangsPeriod
        val date    = _state.value.genAllLangsDate
        val dateKey = computeDateKey(period, date)
        val startMs = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            _state.value = _state.value.copy(genAllLangsLoading = true, genAllLangsResult = null)
            val result = pushService.generateHoroscopes(
                functionUrl = url,
                adminSecret = secret,
                date        = dateKey,
                period      = period.id,
            )
            val durationMs = Clock.System.now().toEpochMilliseconds() - startMs
            val (ok, fail) = result.getOrNull() ?: Pair(0, 0)

            // Сохраняем лог
            val logId = startMs.toString()
            val logEntry = GenerationLogEntry(
                id           = logId,
                timestamp    = startMs,
                period       = period.id,
                dateKey      = dateKey,
                success      = if (result.isSuccess) ok else 0,
                failed       = if (result.isSuccess) fail else 36,
                durationMs   = durationMs,
                triggeredBy  = "manual",
            )
            firebase.saveGenerationLog(logEntry)

            _state.value = _state.value.copy(
                genAllLangsLoading = false,
                genAllLangsResult  = if (result.isSuccess) {
                    if (fail == 0) "ok:$ok" else "ok:$ok fail:$fail"
                } else {
                    result.exceptionOrNull()?.message ?: "Error"
                },
                generationLogs = (listOf(logEntry) + _state.value.generationLogs).take(30),
            )
        }
    }

    fun clearGenAllLangsResult() {
        _state.value = _state.value.copy(genAllLangsResult = null)
    }

    // -- Horoscope Prompt -----------------------------------------------------

    fun loadPrompt() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) return
        val period = _state.value.period.id
        viewModelScope.launch {
            _state.value = _state.value.copy(promptLoading = true, promptError = null)
            val result = pushService.getPrompt(functionUrl = url, adminSecret = secret, period = period)
            _state.value = _state.value.copy(
                promptLoading = false,
                promptText    = result.getOrNull() ?: _state.value.promptText,
                promptError   = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
            )
        }
    }

    fun setPromptText(text: String) {
        _state.value = _state.value.copy(promptText = text, promptSaved = false)
    }

    fun savePrompt() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) return
        val period = _state.value.period.id
        viewModelScope.launch {
            _state.value = _state.value.copy(promptSaving = true, promptError = null, promptSaved = false)
            val result = pushService.setPrompt(
                functionUrl = url,
                adminSecret = secret,
                prompt      = _state.value.promptText,
                period      = period,
            )
            _state.value = _state.value.copy(
                promptSaving = false,
                promptSaved  = result.isSuccess,
                promptError  = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
            )
        }
    }

    fun resetPromptToDefault() {
        _state.value = _state.value.copy(promptText = "", promptSaved = false)
    }

    // -- Schedule -------------------------------------------------------------

    fun loadSchedule() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(scheduleLoading = true, scheduleError = null)
            val result = pushService.getSchedule(functionUrl = url, adminSecret = secret)
            _state.value = _state.value.copy(
                scheduleLoading = false,
                // Сервер хранит локальные часы — показываем как есть
                scheduleHours   = result.getOrNull() ?: _state.value.scheduleHours,
                scheduleError   = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
            )
        }
    }

    fun toggleScheduleHour(hour: Int) {
        val current = _state.value.scheduleHours.toMutableSet()
        if (current.contains(hour)) current.remove(hour) else current.add(hour)
        _state.value = _state.value.copy(scheduleHours = current, scheduleSaved = false)
    }

    fun saveSchedule() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(scheduleSaving = true, scheduleError = null, scheduleSaved = false)
            val result = pushService.setSchedule(
                functionUrl = url,
                adminSecret = secret,
                hours       = _state.value.scheduleHours,
            )
            _state.value = _state.value.copy(
                scheduleSaving = false,
                scheduleSaved  = result.isSuccess,
                scheduleError  = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
            )
        }
    }

    // -- Generation Schedule (авто-генерация гороскопов) ----------------------

    fun loadGenSchedule() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(genScheduleLoading = true, genScheduleError = null)
            val result = pushService.getGenSchedule(functionUrl = url, adminSecret = secret)
            _state.value = _state.value.copy(
                genScheduleLoading = false,
                genScheduleHours   = result.getOrNull() ?: _state.value.genScheduleHours,
                genScheduleError   = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
            )
        }
    }

    fun toggleGenScheduleHour(hour: Int) {
        val current = _state.value.genScheduleHours.toMutableSet()
        if (current.contains(hour)) current.remove(hour) else current.add(hour)
        _state.value = _state.value.copy(genScheduleHours = current, genScheduleSaved = false)
    }

    fun saveGenSchedule() {
        val url    = _state.value.functionUrl.trim()
        val secret = _state.value.adminSecret.trim()
        if (url.isEmpty() || secret.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(genScheduleSaving = true, genScheduleError = null, genScheduleSaved = false)
            val result = pushService.setGenSchedule(
                functionUrl = url,
                adminSecret = secret,
                hours       = _state.value.genScheduleHours,
            )
            _state.value = _state.value.copy(
                genScheduleSaving = false,
                genScheduleSaved  = result.isSuccess,
                genScheduleError  = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
            )
        }
    }

    // -- Generation Logs ------------------------------------------------------

    fun loadGenerationLogs() {
        viewModelScope.launch {
            _state.value = _state.value.copy(logsLoading = true)
            val logs = firebase.getGenerationLogs(30)
            _state.value = _state.value.copy(logsLoading = false, generationLogs = logs)
        }
    }
}

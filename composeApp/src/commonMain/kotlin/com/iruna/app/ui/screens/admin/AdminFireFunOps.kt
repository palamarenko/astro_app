package com.iruna.app.ui.screens.admin

import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import com.iruna.app.notifications.sendLocalTestPush
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

// ── Push notifications ────────────────────────────────────────────────────────

fun AdminViewModel.setFunctionUrl(url: String) {
    _state.value = _state.value.copy(functionUrl = url, pushResult = null)
    UserStorage.load()?.let { UserStorage.save(it.copy(adminFunctionUrl = url)) }
}

fun AdminViewModel.setAdminSecret(secret: String) {
    _state.value = _state.value.copy(adminSecret = secret, pushResult = null)
    UserStorage.load()?.let { UserStorage.save(it.copy(adminSecret = secret)) }
}

fun AdminViewModel.sendPushToSelf() {
    val profile  = UserStorage.load()
    val signId   = profile?.signId ?: "leo"
    val signName = ALL_SIGNS.find { it.id == signId }?.name ?: signId
    sendLocalTestPush(signName)
    _state.value = _state.value.copy(pushResult = "ok_self")
}

fun AdminViewModel.sendPushToAll() {
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

fun AdminViewModel.clearPushResult() {
    _state.value = _state.value.copy(pushResult = null)
}

// ── Generate All Languages ────────────────────────────────────────────────────

fun AdminViewModel.setGenAllLangsPeriod(period: HoroscopePeriod) {
    _state.value = _state.value.copy(genAllLangsPeriod = period, genAllLangsResult = null)
}

fun AdminViewModel.navigateGenAllLangsDate(forward: Boolean) {
    val d = _state.value.genAllLangsDate
    val newDate: LocalDate = when (_state.value.genAllLangsPeriod) {
        HoroscopePeriod.DAILY   -> if (forward) d.plus(1, DateTimeUnit.DAY)  else d.plus(-1, DateTimeUnit.DAY)
        HoroscopePeriod.WEEKLY  -> if (forward) d.plus(7, DateTimeUnit.DAY)  else d.plus(-7, DateTimeUnit.DAY)
        HoroscopePeriod.MONTHLY -> {
            val m = d.monthNumber; val y = d.year
            if (forward) { if (m == 12) LocalDate(y + 1, 1, 1) else LocalDate(y, m + 1, 1) }
            else         { if (m == 1)  LocalDate(y - 1, 12, 1) else LocalDate(y, m - 1, 1) }
        }
    }
    _state.value = _state.value.copy(genAllLangsDate = newDate, genAllLangsResult = null)
}

fun AdminViewModel.generateAllLanguages() {
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
    val langs   = listOf("ru", "uk", "en", "es", "de", "fr")

    viewModelScope.launch {
        _state.value = _state.value.copy(
            genAllLangsLoading     = true,
            genAllLangsResult      = null,
            genAllLangsCurrentLang = null,
            genAllLangsDone        = 0,
        )

        var totalOk   = 0
        var totalFail = 0

        for ((index, lang) in langs.withIndex()) {
            // Обновляем UI: текущий язык и прогресс
            _state.value = _state.value.copy(
                genAllLangsCurrentLang = lang,
                genAllLangsDone        = index,
            )

            val result = pushService.generateHoroscopes(
                functionUrl = url,
                adminSecret = secret,
                date        = dateKey,
                period      = period.id,
                lang        = lang,
            )
            val (ok, fail) = result.getOrNull() ?: Pair(0, 0)

            if (result.isSuccess) {
                totalOk   += ok
                totalFail += fail
                // Сохраняем мету сразу после успешной генерации языка
                if (fail == 0) {
                    firebase.saveHoroscopeMeta(lang, period.id, dateKey, 12)
                }
            } else {
                totalFail += 12
            }
        }

        val durationMs = Clock.System.now().toEpochMilliseconds() - startMs
        val logEntry = GenerationLogEntry(
            id          = startMs.toString(),
            timestamp   = startMs,
            period      = period.id,
            dateKey     = dateKey,
            success     = totalOk,
            failed      = totalFail,
            durationMs  = durationMs,
            triggeredBy = "manual",
        )
        firebase.saveGenerationLog(logEntry)

        _state.value = _state.value.copy(
            genAllLangsLoading     = false,
            genAllLangsCurrentLang = null,
            genAllLangsDone        = langs.size,
            genAllLangsResult      = if (totalFail == 0) "ok:$totalOk" else "ok:$totalOk fail:$totalFail",
            generationLogs         = (listOf(logEntry) + _state.value.generationLogs).take(30),
        )
    }
}

fun AdminViewModel.clearGenAllLangsResult() {
    _state.value = _state.value.copy(genAllLangsResult = null)
}

// ── Horoscope Prompt ──────────────────────────────────────────────────────────

fun AdminViewModel.loadPrompt() {
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

fun AdminViewModel.setPromptText(text: String) {
    _state.value = _state.value.copy(promptText = text, promptSaved = false)
}

fun AdminViewModel.savePrompt() {
    val url    = _state.value.functionUrl.trim()
    val secret = _state.value.adminSecret.trim()
    if (url.isEmpty() || secret.isEmpty()) return
    viewModelScope.launch {
        _state.value = _state.value.copy(promptSaving = true, promptError = null, promptSaved = false)
        val result = pushService.setPrompt(functionUrl = url, adminSecret = secret, prompt = _state.value.promptText, period = _state.value.period.id)
        _state.value = _state.value.copy(
            promptSaving = false,
            promptSaved  = result.isSuccess,
            promptError  = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
        )
    }
}

fun AdminViewModel.resetPromptToDefault() {
    _state.value = _state.value.copy(promptText = "", promptSaved = false)
}

// ── Push schedule ─────────────────────────────────────────────────────────────

fun AdminViewModel.loadSchedule() {
    val url    = _state.value.functionUrl.trim()
    val secret = _state.value.adminSecret.trim()
    if (url.isEmpty() || secret.isEmpty()) return
    viewModelScope.launch {
        _state.value = _state.value.copy(scheduleLoading = true, scheduleError = null)
        val result = pushService.getSchedule(functionUrl = url, adminSecret = secret)
        _state.value = _state.value.copy(
            scheduleLoading = false,
            scheduleHours   = result.getOrNull() ?: _state.value.scheduleHours,
            scheduleError   = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
        )
    }
}

fun AdminViewModel.toggleScheduleHour(hour: Int) {
    val current = _state.value.scheduleHours.toMutableSet()
    if (current.contains(hour)) current.remove(hour) else current.add(hour)
    _state.value = _state.value.copy(scheduleHours = current, scheduleSaved = false)
}

fun AdminViewModel.saveSchedule() {
    val url    = _state.value.functionUrl.trim()
    val secret = _state.value.adminSecret.trim()
    if (url.isEmpty() || secret.isEmpty()) return
    viewModelScope.launch {
        _state.value = _state.value.copy(scheduleSaving = true, scheduleError = null, scheduleSaved = false)
        val result = pushService.setSchedule(functionUrl = url, adminSecret = secret, hours = _state.value.scheduleHours)
        _state.value = _state.value.copy(
            scheduleSaving = false,
            scheduleSaved  = result.isSuccess,
            scheduleError  = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
        )
    }
}

// ── Generation schedule ───────────────────────────────────────────────────────

fun AdminViewModel.loadGenSchedule() {
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

fun AdminViewModel.toggleGenScheduleHour(hour: Int) {
    val current = _state.value.genScheduleHours.toMutableSet()
    if (current.contains(hour)) current.remove(hour) else current.add(hour)
    _state.value = _state.value.copy(genScheduleHours = current, genScheduleSaved = false)
}

fun AdminViewModel.saveGenSchedule() {
    val url    = _state.value.functionUrl.trim()
    val secret = _state.value.adminSecret.trim()
    if (url.isEmpty() || secret.isEmpty()) return
    viewModelScope.launch {
        _state.value = _state.value.copy(genScheduleSaving = true, genScheduleError = null, genScheduleSaved = false)
        val result = pushService.setGenSchedule(functionUrl = url, adminSecret = secret, hours = _state.value.genScheduleHours)
        _state.value = _state.value.copy(
            genScheduleSaving = false,
            genScheduleSaved  = result.isSuccess,
            genScheduleError  = if (result.isFailure) (result.exceptionOrNull()?.message ?: "Error") else null,
        )
    }
}

// ── Generation logs ───────────────────────────────────────────────────────────

fun AdminViewModel.loadGenerationLogs() {
    viewModelScope.launch {
        _state.value = _state.value.copy(logsLoading = true)
        val logs = firebase.getGenerationLogs(30)
        _state.value = _state.value.copy(logsLoading = false, generationLogs = logs)
    }
}

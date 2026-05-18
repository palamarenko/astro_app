package com.iruna.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.iruna.app.data.*
import com.iruna.app.notifications.PushAdminService
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class AdminViewModel(internal val api: ClaudeApiClient) : ViewModel() {
    internal val firebase    = FirebaseService()
    internal val pushService = PushAdminService(
        createHttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000L
                connectTimeoutMillis =  15_000L
            }
        }
    )
    internal val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()
    internal var loadJob: Job? = null

    init {
        val saved = UserStorage.load()
        _state.value = _state.value.copy(
            functionUrl = saved?.adminFunctionUrl ?: "",
            adminSecret = saved?.adminSecret ?: "",
        )
    }

    fun loadAll() {
        load()
        loadPrompt()
        loadGenerationLogs()
    }

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(
            period = period, isLoaded = false, savedCount = -1, saveError = null,
            promptText = "", promptSaved = false, promptError = null,
        )
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
                if (forward) { if (m == 12) LocalDate(y + 1, 1, 1) else LocalDate(y, m + 1, 1) }
                else         { if (m == 1)  LocalDate(y - 1, 12, 1) else LocalDate(y, m - 1, 1) }
            }
        }
        _state.value = _state.value.copy(selectedDate = newDate, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }
}

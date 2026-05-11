package com.astro.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.PlaceDetails
import com.astro.app.data.PlaceFormatting
import com.astro.app.data.PlacePrediction
import com.astro.app.data.PlacesApiClient
import com.astro.app.googleMapsApiKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI state ──────────────────────────────────────────────────────────────────

data class PlacePickerUiState(
    val query: String = "",
    val suggestions: List<PlacePrediction> = emptyList(),
    val isSearching: Boolean = false,
    val selected: PlacePrediction? = null,
    val selectedDetails: PlaceDetails? = null,
    val isFetchingDetails: Boolean = false,
) {
    /** Спиннер в поле поиска. */
    val isLoading: Boolean get() = isSearching || isFetchingDetails

    /** Показывать список подсказок. */
    val showSuggestions: Boolean get() = suggestions.isNotEmpty() && selected == null

    /** Выбранное место уже имеет координаты. */
    val hasCoords: Boolean
        get() = selectedDetails != null &&
            (selectedDetails.lat != 0.0 || selectedDetails.lng != 0.0)

    /** Кнопка «Подтвердить» активна только если место выбрано из списка подсказок. */
    val canConfirm: Boolean
        get() = selected != null && !isFetchingDetails

    /** URL статической карты Google или null, если координаты ещё не загружены. */
    fun staticMapUrl(apiKey: String): String? {
        if (!hasCoords) return null
        val lat = selectedDetails!!.lat
        val lng = selectedDetails.lng

        // Стили — тёмная тема с золотыми границами
        val styles = listOf(
            // Базовый фон
            "feature:all|element:geometry|color:0x12122a",
            // Текстовые метки
            "feature:all|element:labels.text.stroke|color:0x0a0a1a|weight:3",
            "feature:all|element:labels.text.fill|color:0xc8a84b",
            "feature:all|element:labels.icon|visibility:off",
            // Вода
            "feature:water|element:geometry|color:0x0b1628",
            "feature:water|element:labels.text.fill|color:0x4a6fa5",
            // Ландшафт
            "feature:landscape|element:geometry|color:0x15152e",
            "feature:landscape.natural|element:geometry|color:0x161628",
            // ── Административные границы ──────────────────────────────────
            // Государственные границы — жёлто-золотые, жирные
            "feature:administrative.country|element:geometry.stroke|color:0xd4af37|weight:2|visibility:on",
            // Региональные границы — приглушённое золото
            "feature:administrative.province|element:geometry.stroke|color:0x8b7536|weight:1.2|visibility:on",
            // Города — маленькая точка
            "feature:administrative.locality|element:labels.text.fill|color:0xe8d48b|visibility:on",
            // Дороги — едва заметны, не отвлекают
            "feature:road|element:geometry|color:0x1e1e3f",
            "feature:road.highway|element:geometry|color:0x2a2a5a|weight:0.8",
            "feature:road|element:labels|visibility:off",
            // Убираем лишнее
            "feature:poi|visibility:off",
            "feature:transit|visibility:off",
        ).joinToString("&") { s ->
            "style=" + s.replace("|", "%7C")
        }

        return "https://maps.googleapis.com/maps/api/staticmap" +
            "?center=$lat,$lng&zoom=12&size=640x960&scale=2" +
            "&markers=color:red%7Csize:mid%7Clabel:%E2%80%A2%7C$lat,$lng" +
            "&$styles" +
            "&key=$apiKey"
    }
}

// ── Результат подтверждения ───────────────────────────────────────────────────

data class PlacePickerResult(
    val name: String,
    val lat: Double,
    val lng: Double,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PlacePickerViewModel : ViewModel() {

    private val client = PlacesApiClient(googleMapsApiKey)

    private val _state = MutableStateFlow(PlacePickerUiState())
    val state: StateFlow<PlacePickerUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Инициализирует пикер с начальным значением.
     * Если переданы координаты (lat/lng != 0) — карта показывается сразу,
     * без повторного запроса к API.
     */
    fun initialize(initialValue: String, initialLat: Double = 0.0, initialLng: Double = 0.0) {
        searchJob?.cancel()
        val hasPlace = initialValue.isNotBlank()
        val selected = if (hasPlace)
            PlacePrediction(initialValue, "", PlaceFormatting(initialValue, ""))
        else null
        val details = if (hasPlace && (initialLat != 0.0 || initialLng != 0.0))
            PlaceDetails(name = initialValue, lat = initialLat, lng = initialLng)
        else null
        _state.value = PlacePickerUiState(
            query = initialValue,
            selected = selected,
            selectedDetails = details,
        )
    }

    /**
     * Обновляет текст запроса и запускает дебаунс-поиск (400 мс).
     */
    fun setQuery(query: String) {
        _state.update { it.copy(query = query, selected = null, selectedDetails = null, suggestions = emptyList()) }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _state.update { it.copy(isSearching = true) }
            val results = client.autocomplete(query)
            _state.update { it.copy(suggestions = results, isSearching = false) }
        }
    }

    /**
     * Выбирает подсказку из списка и сразу запрашивает координаты.
     */
    fun selectPrediction(pred: PlacePrediction) {
        val needsDetails = pred.placeId.isNotBlank()
        _state.update {
            it.copy(
                query = pred.description,
                selected = pred,
                suggestions = emptyList(),
                selectedDetails = null,
                isFetchingDetails = needsDetails,
            )
        }
        if (needsDetails) {
            viewModelScope.launch {
                val details = client.placeDetails(pred.placeId)
                _state.update { it.copy(selectedDetails = details, isFetchingDetails = false) }
            }
        }
    }

    /**
     * Сбрасывает всё состояние к начальному.
     */
    fun clearQuery() {
        searchJob?.cancel()
        _state.value = PlacePickerUiState()
    }

    /**
     * Возвращает итоговый результат на основе текущего состояния,
     * или null если подтверждение невозможно (canConfirm == false).
     */
    fun buildResult(): PlacePickerResult? {
        val s = _state.value
        if (!s.canConfirm) return null
        val pred = s.selected ?: return null
        return if (s.selectedDetails != null)
            PlacePickerResult(pred.description, s.selectedDetails.lat, s.selectedDetails.lng)
        else
            PlacePickerResult(pred.description, 0.0, 0.0)
    }
}

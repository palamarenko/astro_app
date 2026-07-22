package com.iruna.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminDayCardUiState(
    val lang: String = "ru",
    val cards: Map<String, DayCardContent> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = -1,
    val saveError: String? = null,
    val loadError: String? = null,
    val isLoaded: Boolean = false,
    val generatingCardKeys: Set<String> = emptySet(),
)

class AdminDayCardViewModel(private val api: AiGenerationService) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(AdminDayCardUiState())
    val state: StateFlow<AdminDayCardUiState> = _state.asStateFlow()

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadError = null)
            try {
                val loaded = firebase.getAllDayCards(_state.value.lang)
                _state.value = _state.value.copy(
                    cards = loaded ?: emptyMap(),
                    isLoading = false,
                    isLoaded = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, loadError = e.message ?: "Load error")
            }
        }
    }

    fun updateText(cardKey: String, text: String) {
        val cards = _state.value.cards.toMutableMap()
        cards[cardKey] = (cards[cardKey] ?: DayCardContent()).copy(text = text)
        _state.value = _state.value.copy(cards = cards)
    }

    fun generateForCard(card: TarotCard) {
        if (_state.value.generatingCardKeys.contains(card.resourceKey)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(generatingCardKeys = _state.value.generatingCardKeys + card.resourceKey)
            try {
                val content = api.generateAdminDayCard(card, _state.value.lang)
                val cards = _state.value.cards.toMutableMap()
                cards[card.resourceKey] = (cards[card.resourceKey] ?: DayCardContent()).copy(text = content.text)
                _state.value = _state.value.copy(
                    cards = cards,
                    generatingCardKeys = _state.value.generatingCardKeys - card.resourceKey
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(generatingCardKeys = _state.value.generatingCardKeys - card.resourceKey)
            }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, savedCount = -1, saveError = null)
            try {
                var count = 0
                _state.value.cards.forEach { (key, content) ->
                    firebase.saveDayCard(_state.value.lang, key, content)
                    count++
                }
                _state.value = _state.value.copy(isSaving = false, savedCount = count)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, saveError = e.message ?: "Save error")
            }
        }
    }
}

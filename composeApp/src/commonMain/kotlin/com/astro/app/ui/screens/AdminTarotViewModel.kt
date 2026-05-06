package com.astro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminTarotUiState(
    val lang: String = "ru",
    val cards: Map<String, TarotCardContent> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = -1,
    val saveError: String? = null,
    val loadError: String? = null,
    val isLoaded: Boolean = false,
    val generatingCardKeys: Set<String> = emptySet(),
)

class AdminTarotViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(AdminTarotUiState())
    val state: StateFlow<AdminTarotUiState> = _state.asStateFlow()

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadError = null)
            try {
                val loaded = firebase.getAllTarotCards(_state.value.lang)
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

    fun updateField(cardKey: String, field: String, text: String) {
        val cards = _state.value.cards.toMutableMap()
        val cur = cards[cardKey] ?: TarotCardContent()
        cards[cardKey] = when (field) {
            "past"    -> cur.copy(past = text)
            "present" -> cur.copy(present = text)
            "future"  -> cur.copy(future = text)
            else -> cur
        }
        _state.value = _state.value.copy(cards = cards)
    }

    fun generateForCard(card: TarotCard) {
        if (_state.value.generatingCardKeys.contains(card.resourceKey)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(generatingCardKeys = _state.value.generatingCardKeys + card.resourceKey)
            try {
                val content = api.generateAdminTarotCard(card, _state.value.lang)
                val cards = _state.value.cards.toMutableMap()
                val cur = cards[card.resourceKey] ?: TarotCardContent()
                cards[card.resourceKey] = cur.copy(
                    past    = content.past,
                    present = content.present,
                    future  = content.future,
                )
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
                    firebase.saveTarotCard(_state.value.lang, key, content)
                    count++
                }
                _state.value = _state.value.copy(isSaving = false, savedCount = count)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, saveError = e.message ?: "Save error")
            }
        }
    }
}

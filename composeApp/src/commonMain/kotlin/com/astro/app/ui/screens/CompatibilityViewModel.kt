package com.astro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import com.astro.app.i18n.AppLanguage
import com.astro.app.i18n.getSystemLanguageCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompatibilityUiState(
    val sign1: ZodiacSign? = null,
    val sign2: ZodiacSign? = null,
    val result: CompatibilityResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class CompatibilityViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val _state = MutableStateFlow(CompatibilityUiState())
    val state: StateFlow<CompatibilityUiState> = _state.asStateFlow()

    private val lang: String
        get() = when (AppLanguage.fromCode(getSystemLanguageCode())) {
            AppLanguage.UK -> "uk"
            AppLanguage.EN -> "en"
            else           -> "ru"
        }

    fun setSign1(sign: ZodiacSign) {
        _state.value = _state.value.copy(sign1 = sign, result = null)
    }

    fun setSign2(sign: ZodiacSign) {
        val s1 = _state.value.sign1 ?: return
        _state.value = _state.value.copy(sign2 = sign)
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, result = null, error = null)
            try {
                val result = api.getCompatibility(s1, sign, lang)
                _state.value = _state.value.copy(result = result, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}

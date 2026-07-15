package com.iruna.app.ui.screens.dream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iruna.app.data.AiGenerationService
import com.iruna.app.data.Track
import com.iruna.app.i18n.AppLanguage
import com.iruna.app.i18n.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

data class DreamUiState(
    val dreamText: String = "",
    val interpretation: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val adMessage: String? = null,
)

class DreamViewModel(private val api: AiGenerationService) : ViewModel() {

    private val _state = MutableStateFlow(DreamUiState())
    val state: StateFlow<DreamUiState> = _state.asStateFlow()

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

    fun onDreamTextChanged(text: String) {
        if (text.length <= 2000) {
            _state.value = _state.value.copy(dreamText = text, error = null)
        }
    }

    fun onAdRewarded() {
        interpret()
    }

    fun onAdFailed(message: String) {
        // Ad unavailable — decode anyway so user is never blocked
        interpret()
    }

    private fun interpret() {
        val text = _state.value.dreamText.trim()
        if (text.isBlank()) return

        Track.dreamDecodeClick(text.length)
        _state.value = _state.value.copy(isLoading = true, error = null, interpretation = null)

        viewModelScope.launch {
            Track.aiGenerationRequest("dream", lang)
            val t0 = TimeSource.Monotonic.markNow()
            val result = try {
                val r = api.getDreamInterpretation(text, lang)
                Track.aiGenerationSuccess("dream", lang, t0.elapsedNow().inWholeMilliseconds)
                r
            } catch (e: Exception) {
                Track.aiGenerationError("dream", lang, e.message ?: "error")
                getFallbackInterpretation(lang)
            }
            Track.dreamResultView()
            _state.value = _state.value.copy(
                interpretation = result,
                isLoading = false,
            )
        }
    }

    fun reset() {
        Track.dreamNew()
        _state.value = DreamUiState()
    }
}

private val FALLBACK_INTERPRETATIONS = mapOf(
    "ru" to "Ваш сон несёт в себе глубокое послание подсознания. Образы, которые вы видели, отражают скрытые желания и внутренние переживания. Символы указывают на период трансформации — что-то старое уходит, освобождая место новому. Доверьтесь своей интуиции, и смысл сна откроется вам в нужный момент.",
    "uk" to "Ваш сон несе в собі глибоке послання підсвідомості. Образи, які ви бачили, відображають приховані бажання та внутрішні переживання. Символи вказують на період трансформації — щось старе відходить, звільняючи місце новому. Довіртеся своїй інтуїції, і сенс сну відкриється вам у потрібний момент.",
    "en" to "Your dream carries a profound message from your subconscious. The images you saw reflect hidden desires and inner experiences. The symbols point to a period of transformation — something old is leaving, making space for the new. Trust your intuition and the meaning of the dream will reveal itself at the right moment.",
    "es" to "Tu sueño lleva un profundo mensaje de tu subconsciente. Las imágenes que viste reflejan deseos ocultos y experiencias internas. Los símbolos apuntan a un período de transformación — algo viejo se va, haciendo espacio para lo nuevo. Confía en tu intuición y el significado del sueño se revelará en el momento indicado.",
    "de" to "Dein Traum trägt eine tiefe Botschaft deines Unterbewusstseins. Die Bilder, die du gesehen hast, spiegeln verborgene Wünsche und innere Erfahrungen wider. Die Symbole deuten auf eine Transformationsphase hin — etwas Altes geht, macht Platz für Neues. Vertraue deiner Intuition und die Bedeutung des Traums wird sich im richtigen Moment enthüllen.",
    "fr" to "Votre rêve porte un message profond de votre subconscient. Les images que vous avez vues reflètent des désirs cachés et des expériences intérieures. Les symboles indiquent une période de transformation — quelque chose d'ancien part, faisant place au nouveau. Faites confiance à votre intuition et le sens du rêve se révélera au bon moment.",
)

private fun getFallbackInterpretation(lang: String): String =
    FALLBACK_INTERPRETATIONS[lang] ?: FALLBACK_INTERPRETATIONS["en"]!!

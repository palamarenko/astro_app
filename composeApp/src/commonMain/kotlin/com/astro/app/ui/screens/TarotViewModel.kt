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

data class TarotUiState(
    val cards: List<TarotCard> = emptyList(),
    val reading: TarotReadingResponse? = null,
    val isLoading: Boolean = false,
    val revealedCount: Int = 0,
    val error: String? = null,
)

private val MOCK_READINGS = listOf(
    TarotReadingResponse(
        past    = "В прошлом вы прошли сквозь испытания, которые закалили ваш дух. Трудности не сломили — они выковали характер и научили ценить то, что действительно важно.",
        present = "Сейчас вы стоите на перекрёстке. Перед вами открывается новая страница — но её нужно перевернуть самостоятельно. Ваша интуиция особенно сильна в эти дни.",
        future  = "Впереди вас ждёт период трансформации и обновления. То, что казалось недостижимым, начнёт проявляться в вашей жизни. Доверьтесь потоку перемен.",
        summary = "Карты складываются в историю возрождения. Вы прошли долгий путь и теперь готовы к следующей главе. Звёзды благоволят вашим начинаниям — действуйте смело, но с мудростью. Вселенная видит ваши усилия и готовится вознаградить вас неожиданным образом."
    ),
    TarotReadingResponse(
        past    = "Прошлое хранит в себе незакрытые истории и невысказанные слова. Но именно этот груз помог вам стать тем, кем вы являетесь сегодня — сильным и мудрым.",
        present = "Сейчас вы находитесь в процессе глубокой внутренней работы. Старое уходит, освобождая место для нового. Не цепляйтесь за то, что уже отслужило своё.",
        future  = "Новый цикл несёт с собой свет и вдохновение. Ваши мечты имеют все шансы воплотиться в реальность — главное, не останавливаться и не сомневаться в себе.",
        summary = "Три карты образуют мощный архетип перерождения. Прошлое было вашим учителем, настоящее — вашей возможностью, а будущее — вашей наградой. Звёзды указывают на особый период в вашей жизни. Оставайтесь открытыми к неожиданным возможностям — они уже на пути к вам."
    ),
    TarotReadingResponse(
        past    = "В прошлом вы отдавали больше, чем получали, и это истощило ваши ресурсы. Но каждая отданная капля энергии была посеяна в почву — скоро она даст ростки.",
        present = "Пришло время заботиться о себе. Расставьте приоритеты и не бойтесь сказать «нет» тому, что вам не служит. Ваша энергия — ваш главный ресурс.",
        future  = "Впереди вас ждёт период изобилия и гармонии. Отношения углубятся, проекты наберут силу, а внутренний покой станет вашим постоянным спутником.",
        summary = "Расклад указывает на цикл отдачи и получения, который наконец возвращается к балансу. Вселенная не забывает тех, кто действует с открытым сердцем. Доверьтесь процессу — всё складывается именно так, как должно. Ваше время пришло."
    ),
    TarotReadingResponse(
        past    = "Прошлый период был отмечен поиском себя и своего места в мире. Этот поиск не был напрасным — каждый шаг привёл вас именно туда, где вы находитесь сейчас.",
        present = "Сейчас вы находитесь на пике своих возможностей. Ваша воля и энергия способны сдвинуть горы — важно лишь направить их в нужное русло.",
        future  = "Предстоящий путь полон возможностей для роста и самовыражения. Не бойтесь быть собой в полной мере — именно ваша уникальность откроет нужные двери.",
        summary = "Ваш расклад говорит о силе духа и непреклонной воле. Карты выстраиваются в историю победы — не над другими, но над собственными страхами и сомнениями. Звёзды видят вашу готовность к переменам и отвечают — пора. Шагните навстречу своей судьбе."
    ),
)

class TarotViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(TarotUiState())
    val state: StateFlow<TarotUiState> = _state.asStateFlow()

    private val lang: String
        get() = when (AppLanguage.fromCode(getSystemLanguageCode())) {
            AppLanguage.UK -> "uk"
            else           -> "ru"
        }

    fun drawCards() {
        val picked = ALL_TAROT.shuffled().take(3).map { card ->
            card.copy(reversed = (0..9).random() > 6)
        }
        _state.value = TarotUiState(cards = picked, isLoading = true, revealedCount = 0)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800L)

            val reading = try {
                val allCards = firebase.getAllTarotCards(lang)
                if (allCards != null) {
                    val c0 = allCards[picked[0].resourceKey] ?: TarotCardContent()
                    val c1 = allCards[picked[1].resourceKey] ?: TarotCardContent()
                    val c2 = allCards[picked[2].resourceKey] ?: TarotCardContent()

                    val pastText    = c0.past
                    val presentText = c1.present
                    val futureText  = c2.future

                    if (pastText.isNotBlank() || presentText.isNotBlank() || futureText.isNotBlank()) {
                        TarotReadingResponse(
                            past    = pastText,
                            present = presentText,
                            future  = futureText,
                            summary = buildSummary(pastText, presentText, futureText)
                        )
                    } else {
                        MOCK_READINGS.random()
                    }
                } else {
                    MOCK_READINGS.random()
                }
            } catch (e: Exception) {
                MOCK_READINGS.random()
            }

            _state.value = _state.value.copy(reading = reading, isLoading = false)
            revealCardsWithDelay()
        }
    }

    private fun buildSummary(past: String, present: String, future: String): String {
        val filled = listOf(past, present, future).count { it.isNotBlank() }
        return if (filled >= 2)
            "Карты открыли свои послания. Прислушайтесь к каждому из них — вместе они складываются в единый путь."
        else ""
    }

    private suspend fun revealCardsWithDelay() {
        repeat(3) { i ->
            kotlinx.coroutines.delay(550L)
            _state.value = _state.value.copy(revealedCount = i + 1)
        }
    }
}

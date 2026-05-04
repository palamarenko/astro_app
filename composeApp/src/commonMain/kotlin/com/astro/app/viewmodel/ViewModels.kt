package com.astro.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

// ── HoroscopeViewModel ────────────────────────────────────────────────────────
data class HoroscopeUiState(
    val selectedSign: ZodiacSign? = null,
    val period: HoroscopePeriod = HoroscopePeriod.DAILY,
    val horoscope: HoroscopeResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val periodPrompt: String = HoroscopePeriod.DAILY.promptRu,
)

class HoroscopeViewModel(
    private val firebase: FirebaseService = FirebaseService()
) : ViewModel() {
    private val _state = MutableStateFlow(HoroscopeUiState())
    val state: StateFlow<HoroscopeUiState> = _state.asStateFlow()

    fun selectSign(sign: ZodiacSign) {
        _state.value = _state.value.copy(selectedSign = sign)
        loadHoroscope(sign, _state.value.period)
    }

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(period = period)
        _state.value.selectedSign?.let { loadHoroscope(it, period) }
    }

    private fun loadHoroscope(sign: ZodiacSign, period: HoroscopePeriod) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, horoscope = null, error = null)
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val date = when (period) {
                    HoroscopePeriod.DAILY -> today.toString()
                    HoroscopePeriod.WEEKLY -> {
                        val year = today.year
                        val week = (today.dayOfYear / 7) + 1
                        "${year}-W${week.toString().padStart(2, '0')}"
                    }
                    HoroscopePeriod.MONTHLY -> "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
                }
                print(date)
                val response = firebase.getHoroscope("ru", period.id, date, sign.id)
                _state.value = _state.value.copy(
                    horoscope = response,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Unknown error", isLoading = false)
            }
        }
    }
}

// ── CompatibilityViewModel ────────────────────────────────────────────────────
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

    fun setSign1(sign: ZodiacSign) { _state.value = _state.value.copy(sign1 = sign, result = null) }

    fun setSign2(sign: ZodiacSign) {
        val s1 = _state.value.sign1 ?: return
        _state.value = _state.value.copy(sign2 = sign)
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, result = null, error = null)
            try {
                val result = api.getCompatibility(s1, sign)
                _state.value = _state.value.copy(result = result, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}

// ── TarotViewModel ────────────────────────────────────────────────────────────
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
    private val _state = MutableStateFlow(TarotUiState())
    val state: StateFlow<TarotUiState> = _state.asStateFlow()

    fun drawCards() {
        val picked = ALL_TAROT.shuffled().take(3).map { card ->
            card.copy(reversed = (0..9).random() > 6)
        }
        _state.value = TarotUiState(cards = picked, isLoading = true, revealedCount = 0)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800L)
            val reading = MOCK_READINGS.random()
            _state.value = _state.value.copy(reading = reading, isLoading = false)
            revealCardsWithDelay()
        }
    }

    private suspend fun revealCardsWithDelay() {
        repeat(3) { i ->
            kotlinx.coroutines.delay(550L)
            _state.value = _state.value.copy(revealedCount = i + 1)
        }
    }
}

// ── ProfileViewModel ──────────────────────────────────────────────────────────
data class ProfileUiState(
    val sign: ZodiacSign = ALL_SIGNS[4],
    val birthDate: String = "",
    val insight: String? = null,
    val isLoading: Boolean = false,
    val showSignPicker: Boolean = false,
)

class ProfileViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { loadInsight(_state.value.sign) }

    fun selectSign(sign: ZodiacSign) {
        _state.value = _state.value.copy(sign = sign, insight = null, showSignPicker = false)
        loadInsight(sign)
    }

    fun setBirthDate(date: String) { _state.value = _state.value.copy(birthDate = date) }

    fun toggleSignPicker() { _state.value = _state.value.copy(showSignPicker = !_state.value.showSignPicker) }

    private fun loadInsight(sign: ZodiacSign) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val text = api.getSignInsight(sign)
                _state.value = _state.value.copy(insight = text, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(insight = null, isLoading = false)
            }
        }
    }
}

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
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

// ── HoroscopeViewModel ────────────────────────────────────────────
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
                    HoroscopePeriod.DAILY   -> today.toString()
                    HoroscopePeriod.WEEKLY  -> {
                        val week = (today.dayOfYear / 7) + 1
                        "${today.year}-W${week.toString().padStart(2, '0')}"
                    }
                    HoroscopePeriod.MONTHLY -> "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
                }
                val response = firebase.getHoroscope("ru", period.id, date, sign.id)
                _state.value = _state.value.copy(horoscope = response, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Unknown error", isLoading = false)
            }
        }
    }
}

// ── CompatibilityViewModel ────────────────────────────────────────────
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

// ── ProfileViewModel ────────────────────────────────────────────
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


    fun selectSign(sign: ZodiacSign) {
        _state.value = _state.value.copy(sign = sign, insight = null, showSignPicker = false)
    }

    fun setBirthDate(date: String) { _state.value = _state.value.copy(birthDate = date) }

    fun toggleSignPicker() { _state.value = _state.value.copy(showSignPicker = !_state.value.showSignPicker) }


}

// ── AdminViewModel ────────────────────────────────────────────────────────────

private fun computeAdminDateKey(period: HoroscopePeriod, date: kotlinx.datetime.LocalDate): String =
    when (period) {
        HoroscopePeriod.DAILY   -> date.toString()
        HoroscopePeriod.WEEKLY  -> {
            val week = (date.dayOfYear / 7) + 1
            "${date.year}-W${week.toString().padStart(2, '0')}"
        }
        HoroscopePeriod.MONTHLY -> "${date.year}-${date.monthNumber.toString().padStart(2, '0')}"
    }

private fun defaultAdminHoroscopes(): Map<String, HoroscopeResponse> =
    ALL_SIGNS.associate { it.id to HoroscopeResponse("", 75, 75, 75, 75) }

data class AdminUiState(
    val lang: String = "ru",
    val period: HoroscopePeriod = HoroscopePeriod.DAILY,
    val selectedDate: kotlinx.datetime.LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val horoscopes: Map<String, HoroscopeResponse> = defaultAdminHoroscopes(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = -1,
    val saveError: String? = null,
    val loadError: String? = null,
    val isLoaded: Boolean = false,
    val generatingSignIds: Set<String> = emptySet(),
)

class AdminViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
    }

    fun setPeriod(period: HoroscopePeriod) {
        _state.value = _state.value.copy(period = period, isLoaded = false, savedCount = -1, saveError = null)
    }

    fun navigateDate(forward: Boolean) {
        val d = _state.value.selectedDate
        val newDate: kotlinx.datetime.LocalDate = when (_state.value.period) {
            HoroscopePeriod.DAILY  ->
                if (forward) d.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
                else         d.plus(-1, kotlinx.datetime.DateTimeUnit.DAY)
            HoroscopePeriod.WEEKLY ->
                if (forward) d.plus(7, kotlinx.datetime.DateTimeUnit.DAY)
                else         d.plus(-7, kotlinx.datetime.DateTimeUnit.DAY)
            HoroscopePeriod.MONTHLY -> {
                val m = d.monthNumber; val y = d.year
                if (forward) {
                    if (m == 12) kotlinx.datetime.LocalDate(y + 1, 1, 1)
                    else         kotlinx.datetime.LocalDate(y, m + 1, 1)
                } else {
                    if (m == 1)  kotlinx.datetime.LocalDate(y - 1, 12, 1)
                    else         kotlinx.datetime.LocalDate(y, m - 1, 1)
                }
            }
        }
        _state.value = _state.value.copy(selectedDate = newDate, isLoaded = false, savedCount = -1, saveError = null)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadError = null, savedCount = -1, saveError = null)
            try {
                val key = computeAdminDateKey(_state.value.period, _state.value.selectedDate)
                val loaded = firebase.getAllSignHoroscopes(_state.value.lang, _state.value.period.id, key)
                val merged = ALL_SIGNS.associate { sign ->
                    sign.id to (loaded?.get(sign.id) ?: HoroscopeResponse("", 75, 75, 75, 75))
                }
                _state.value = _state.value.copy(horoscopes = merged, isLoading = false, isLoaded = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, loadError = e.message ?: "Load error")
            }
        }
    }

    fun updateText(signId: String, text: String) {
        val h = _state.value.horoscopes.toMutableMap()
        h[signId] = (h[signId] ?: HoroscopeResponse("", 75, 75, 75, 75)).copy(text = text)
        _state.value = _state.value.copy(horoscopes = h)
    }

    fun updateScore(signId: String, field: String, delta: Int) {
        val h = _state.value.horoscopes.toMutableMap()
        val cur = h[signId] ?: HoroscopeResponse("", 75, 75, 75, 75)
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
            _state.value = _state.value.copy(
                generatingSignIds = _state.value.generatingSignIds + sign.id
            )
            try {
                val key = computeAdminDateKey(_state.value.period, _state.value.selectedDate)
                val generated = api.generateAdminHoroscope(sign, _state.value.period, _state.value.lang, key)
                val h = _state.value.horoscopes.toMutableMap()
                h[sign.id] = generated
                _state.value = _state.value.copy(
                    horoscopes = h,
                    generatingSignIds = _state.value.generatingSignIds - sign.id
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    generatingSignIds = _state.value.generatingSignIds - sign.id
                )
            }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, savedCount = -1, saveError = null)
            val key = computeAdminDateKey(_state.value.period, _state.value.selectedDate)
            var count = 0
            var hasError = false
            for (sign in ALL_SIGNS) {
                val h = _state.value.horoscopes[sign.id] ?: continue
                if (h.text.isBlank()) continue
                val ok = firebase.saveFullHoroscope(_state.value.lang, _state.value.period.id, key, sign.id, h)
                if (ok) count++ else hasError = true
            }
            _state.value = _state.value.copy(
                isSaving = false,
                savedCount = count,
                saveError = if (hasError) "Some signs failed to save" else null
            )
        }
    }
}



private fun defaultTarotCards(): Map<String, TarotCardContent> =
    ALL_TAROT.associate { it.resourceKey to TarotCardContent() }

data class AdminTarotUiState(
    val lang: String = "ru",
    val cards: Map<String, TarotCardContent> = defaultTarotCards(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = -1,
    val saveError: String? = null,
    val loadError: String? = null,
    val isLoaded: Boolean = false,
    val generatingCardKeys: Set<String> = emptySet(),
)

class AdminTarotViewModel(private val api: ClaudeApiClient? = null) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(AdminTarotUiState())
    val state: StateFlow<AdminTarotUiState> = _state.asStateFlow()

    fun setLang(lang: String) {
        _state.value = _state.value.copy(lang = lang, isLoaded = false, savedCount = -1, saveError = null)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadError = null, savedCount = -1, saveError = null)
            try {
                val loaded = firebase.getAllTarotCards(_state.value.lang)
                val merged = ALL_TAROT.associate { card ->
                    card.resourceKey to (loaded?.get(card.resourceKey) ?: TarotCardContent())
                }
                _state.value = _state.value.copy(cards = merged, isLoading = false, isLoaded = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, loadError = e.message ?: "Load error")
            }
        }
    }

    fun generateForCard(card: TarotCard) {
        val client = api ?: return
        if (_state.value.generatingCardKeys.contains(card.resourceKey)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                generatingCardKeys = _state.value.generatingCardKeys + card.resourceKey
            )
            try {
                val generated = client.generateAdminTarotCard(card, _state.value.lang)
                val m = _state.value.cards.toMutableMap()
                m[card.resourceKey] = generated
                _state.value = _state.value.copy(
                    cards = m,
                    generatingCardKeys = _state.value.generatingCardKeys - card.resourceKey
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    generatingCardKeys = _state.value.generatingCardKeys - card.resourceKey
                )
            }
        }
    }

    fun updateField(cardKey: String, field: String, text: String) {
        val m = _state.value.cards.toMutableMap()
        val cur = m[cardKey] ?: TarotCardContent()
        m[cardKey] = when (field) {
            "past"    -> cur.copy(past    = text)
            "present" -> cur.copy(present = text)
            else      -> cur.copy(future  = text)
        }
        _state.value = _state.value.copy(cards = m)
    }

    fun saveAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, savedCount = -1, saveError = null)
            var count = 0
            var hasError = false
            for (card in ALL_TAROT) {
                val c = _state.value.cards[card.resourceKey] ?: continue
                if (c.past.isBlank() && c.present.isBlank() && c.future.isBlank()) continue
                val ok = firebase.saveTarotCard(_state.value.lang, card.resourceKey, c)
                if (ok) count++ else hasError = true
            }
            _state.value = _state.value.copy(
                isSaving = false,
                savedCount = count,
                saveError = if (hasError) "Some cards failed to save" else null
            )
        }
    }
}

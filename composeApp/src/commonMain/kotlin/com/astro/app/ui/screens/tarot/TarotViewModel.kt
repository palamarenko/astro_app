package com.astro.app.ui.screens.tarot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.app.data.*
import com.astro.app.i18n.AppLanguage
import com.astro.app.i18n.getSystemLanguageCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TarotUiState(
    val cards: List<TarotCard> = emptyList(),
    val reading: TarotReadingResponse? = null,
    val isLoading: Boolean = false,
    val revealedCount: Int = 0,
    val error: String? = null,
    /** true — пользователь уже сделал расклад и может посмотреть рекламу для нового. */
    val canWatchAd: Boolean = false,
    /** Временное сообщение о статусе рекламы (показывается и исчезает). */
    val adMessage: String? = null,
    /** Выбранный период (null = показываем список периодов). */
    val currentPeriod: HoroscopePeriod? = null,
    /** Сохранённые расклады по периодам: period.id → снимок */
    val periodSnapshots: Map<String, TarotPersistState> = emptyMap(),
)

private val MOCK_READINGS = listOf(
    TarotReadingResponse(
        past    = "В прошлом вы прошли сквозь испытания, которые закалили ваш дух. Трудности не сломили — они выковали характер и научили ценить то, что действительно важно.",
        present = "Сейчас вы стоите на перекрёстке. Перед вами открывается новая страница — но её нужно перевернуть самостоятельно. Ваша интуиция особенно сильна в эти дни.",
        future  = "Впереди вас ждёт период трансформации и обновления. То, что казалось недостижимым, начнёт проявляться в вашей жизни. Доверьтесь потоку перемен.",
        summary = "Карты складываются в историю возрождения. Вы прошли долгий путь и теперь готовы к следующей главе. Звёзды благоволят вашим начинаниям — действуйте смело, но с мудростью."
    ),
    TarotReadingResponse(
        past    = "Прошлое хранит в себе незакрытые истории и невысказанные слова. Но именно этот груз помог вам стать тем, кем вы являетесь сегодня — сильным и мудрым.",
        present = "Сейчас вы находитесь в процессе глубокой внутренней работы. Старое уходит, освобождая место для нового. Не цепляйтесь за то, что уже отслужило своё.",
        future  = "Новый цикл несёт с собой свет и вдохновение. Ваши мечты имеют все шансы воплотиться в реальность — главное, не останавливаться и не сомневаться в себе.",
        summary = "Три карты образуют мощный архетип перерождения. Прошлое было вашим учителем, настоящее — вашей возможностью, а будущее — вашей наградой."
    ),
    TarotReadingResponse(
        past    = "В прошлом вы отдавали больше, чем получали, и это истощило ваши ресурсы. Но каждая отданная капля энергии была посеяна в почву — скоро она даст ростки.",
        present = "Пришло время заботиться о себе. Расставьте приоритеты и не бойтесь сказать «нет» тому, что вам не служит. Ваша энергия — ваш главный ресурс.",
        future  = "Впереди вас ждёт период изобилия и гармонии. Отношения углубятся, проекты наберут силу, а внутренний покой станет вашим постоянным спутником.",
        summary = "Расклад указывает на цикл отдачи и получения, который наконец возвращается к балансу. Вселенная не забывает тех, кто действует с открытым сердцем."
    ),
)

/** Randomly selects which personal fields to include in this reading. */
private fun buildPersonalContext(profile: UserProfile?): TarotPersonalContext? {
    if (profile == null) return null

    val name       = profile.name.takeIf { it.isNotBlank() && Random.nextFloat() < 0.60f }
    val gender     = profile.gender.takeIf { it.isNotBlank() }
    val birthSign  = profile.signId.takeIf { it.isNotBlank() && Random.nextFloat() < 0.45f }
    val birthPlace = profile.birthPlace.takeIf { it.isNotBlank() && Random.nextFloat() < 0.40f }

    val birthDate: String? = if (
        profile.birthDay > 0 && profile.birthMonth > 0 && Random.nextFloat() < 0.45f
    ) {
        val day   = profile.birthDay.toString().padStart(2, '0')
        val month = profile.birthMonth.toString().padStart(2, '0')
        if (profile.birthYear > 0) "$day.$month.${profile.birthYear}" else "$day.$month"
    } else null

    if (name == null && gender == null && birthDate == null && birthSign == null && birthPlace == null) return null

    return TarotPersonalContext(
        name      = name,
        gender    = gender,
        birthDate = birthDate,
        birthSign = birthSign,
        birthPlace = birthPlace,
    )
}

/** Возвращает строку-ключ периода для сравнения с датой в TarotPersistState */
private fun periodCurrentKey(period: HoroscopePeriod): String = when (period) {
    HoroscopePeriod.DAILY   -> TarotStorage.todayKey()
    HoroscopePeriod.WEEKLY  -> TarotStorage.weekKey()
    HoroscopePeriod.MONTHLY -> TarotStorage.monthKey()
}

class TarotViewModel(private val api: ClaudeApiClient) : ViewModel() {
    private val firebase = FirebaseService()
    private val _state = MutableStateFlow(TarotUiState())
    val state: StateFlow<TarotUiState> = _state.asStateFlow()

    private val lang: String
        get() = when (AppLanguage.fromCode(getSystemLanguageCode())) {
            AppLanguage.UK -> "uk"
            AppLanguage.RU -> "ru"
            else           -> "en"
        }

    init {
        loadSavedReadings()
    }

    /** Загружает актуальные сохранённые расклады для всех периодов. */
    fun loadSavedReadings() {
        val snapshots = mutableMapOf<String, TarotPersistState>()
        HoroscopePeriod.entries.forEach { period ->
            val saved = TarotStorage.loadPeriod(period.id)
            if (saved != null && saved.date == periodCurrentKey(period)) {
                snapshots[period.id] = saved
            }
        }
        _state.value = _state.value.copy(periodSnapshots = snapshots)
    }

    /** Выбирает период: если есть актуальное сохранение — восстанавливает, иначе пустой экран. */
    fun selectPeriod(period: HoroscopePeriod) {
        val saved = TarotStorage.loadPeriod(period.id)
        val snapshots = _state.value.periodSnapshots

        if (saved != null && saved.date == periodCurrentKey(period)) {
            val cards = saved.cards.mapNotNull { snap ->
                ALL_TAROT.find { it.number == snap.number }?.copy(reversed = snap.reversed)
            }
            _state.value = TarotUiState(
                currentPeriod   = period,
                cards           = cards,
                reading         = saved.reading,
                revealedCount   = 3,
                canWatchAd      = true,
                periodSnapshots = snapshots,
            )
        } else {
            _state.value = TarotUiState(
                currentPeriod   = period,
                periodSnapshots = snapshots,
            )
        }
    }

    /** Возврат к списку периодов. */
    fun clearPeriod() {
        val snapshots = _state.value.periodSnapshots
        _state.value = TarotUiState(periodSnapshots = snapshots)
    }

    fun drawCards() {
        val period = _state.value.currentPeriod
        val snapshots = _state.value.periodSnapshots
        val picked = ALL_TAROT.shuffled().take(3).map { card ->
            card.copy(reversed = (0..9).random() > 6)
        }
        _state.value = TarotUiState(
            cards           = picked,
            isLoading       = true,
            revealedCount   = 0,
            currentPeriod   = period,
            periodSnapshots = snapshots,
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(1800L)

            val reading = try {
                val mock = MOCK_READINGS.random()
                val allCards = firebase.getAllTarotCards(lang)
                val pastText    = allCards?.get(picked[0].resourceKey)?.past    ?: mock.past
                val presentText = allCards?.get(picked[1].resourceKey)?.present ?: mock.present
                val futureText  = allCards?.get(picked[2].resourceKey)?.future  ?: mock.future

                val profile = UserStorage.load()
                val context = buildPersonalContext(profile)
                val summary = try {
                    api.getTarotSummary(picked, context, lang)
                } catch (e: Exception) {
                    mock.summary
                }

                TarotReadingResponse(
                    past    = pastText,
                    present = presentText,
                    future  = futureText,
                    summary = summary,
                )
            } catch (e: Exception) {
                MOCK_READINGS.random()
            }

            // Сохраняем расклад в хранилище для текущего периода
            if (period != null) {
                val dateKey = periodCurrentKey(period)
                val snapshot = TarotPersistState(
                    date    = dateKey,
                    cards   = picked.map { TarotCardSnapshot(it.number, it.reversed) },
                    reading = reading,
                )
                TarotStorage.savePeriod(period.id, snapshot)
                val newSnapshots = _state.value.periodSnapshots.toMutableMap()
                newSnapshots[period.id] = snapshot
                _state.value = _state.value.copy(
                    reading         = reading,
                    isLoading       = false,
                    canWatchAd      = true,
                    periodSnapshots = newSnapshots,
                )
            } else {
                _state.value = _state.value.copy(
                    reading    = reading,
                    isLoading  = false,
                    canWatchAd = true,
                )
            }
            revealCardsWithDelay()
        }
    }

    fun onAdRewarded() {
        drawCards()
    }

    fun onAdFailed(message: String) {
        _state.value = _state.value.copy(adMessage = message)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000L)
            _state.value = _state.value.copy(adMessage = null)
        }
    }

    private suspend fun revealCardsWithDelay() {
        repeat(3) { i ->
            kotlinx.coroutines.delay(550L)
            _state.value = _state.value.copy(revealedCount = i + 1)
        }
    }
}

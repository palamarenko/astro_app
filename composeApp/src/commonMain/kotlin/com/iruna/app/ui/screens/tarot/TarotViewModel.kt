package com.iruna.app.ui.screens.tarot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iruna.app.data.*
import com.iruna.app.i18n.AppLanguage
import com.iruna.app.i18n.LanguageManager
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

private val MOCK_READINGS = mapOf(
    "ru" to listOf(
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
    ),
    "uk" to listOf(
        TarotReadingResponse(
            past    = "У минулому ви пройшли крізь випробування, які загартували ваш дух. Труднощі не зламали — вони викували характер і навчили цінувати те, що справді важливо.",
            present = "Зараз ви стоїте на роздоріжжі. Перед вами відкривається нова сторінка — але її треба перегорнути самостійно. Ваша інтуїція особливо сильна в ці дні.",
            future  = "Попереду на вас чекає період трансформації та оновлення. Те, що здавалося недосяжним, почне проявлятися у вашому житті. Довіртеся потоку змін.",
            summary = "Карти складаються в історію відродження. Ви пройшли довгий шлях і тепер готові до наступного розділу. Зірки сприяють вашим починанням — дійте сміливо, але з мудрістю."
        ),
        TarotReadingResponse(
            past    = "Минуле зберігає в собі незакриті історії та невимовлені слова. Але саме цей тягар допоміг вам стати тим, ким ви є сьогодні — сильним і мудрим.",
            present = "Зараз ви перебуваєте в процесі глибокої внутрішньої роботи. Старе іде, звільняючи місце для нового. Не чіпляйтеся за те, що вже відслужило своє.",
            future  = "Новий цикл несе з собою світло та натхнення. Ваші мрії мають усі шанси втілитися в реальність — головне, не зупинятися й не сумніватися в собі.",
            summary = "Три карти утворюють потужний архетип переродження. Минуле було вашим учителем, сьогодення — вашою можливістю, а майбутнє — вашою нагородою."
        ),
        TarotReadingResponse(
            past    = "У минулому ви віддавали більше, ніж отримували, і це виснажило ваші ресурси. Але кожна віддана крапля енергії була посіяна в ґрунт — незабаром вона дасть паростки.",
            present = "Прийшов час дбати про себе. Розставте пріоритети і не бійтеся сказати «ні» тому, що вам не служить. Ваша енергія — ваш головний ресурс.",
            future  = "Попереду вас чекає період достатку та гармонії. Стосунки поглибляться, проєкти наберуть силу, а внутрішній спокій стане вашим постійним супутником.",
            summary = "Розклад вказує на цикл віддачі та отримання, який нарешті повертається до балансу. Всесвіт не забуває тих, хто діє з відкритим серцем."
        ),
    ),
    "en" to listOf(
        TarotReadingResponse(
            past    = "In the past, you endured trials that forged your spirit. Hardships didn't break you — they shaped your character and taught you to cherish what truly matters.",
            present = "You stand at a crossroads. A new chapter unfolds before you — but you must turn the page yourself. Your intuition is especially powerful in these days.",
            future  = "A period of transformation and renewal awaits. What once seemed out of reach will begin to manifest in your life. Trust the current of change.",
            summary = "The cards weave a story of rebirth. You have walked a long path and are now ready for the next chapter. The stars favor your endeavors — act boldly, yet with wisdom."
        ),
        TarotReadingResponse(
            past    = "The past holds unfinished stories and unspoken words. Yet that very weight helped you become who you are today — strong and wise.",
            present = "You are in the midst of deep inner work. The old is departing, making room for the new. Release what has already served its purpose.",
            future  = "A new cycle brings light and inspiration. Your dreams have every chance of becoming reality — the key is to keep moving and believe in yourself.",
            summary = "Three cards form a powerful archetype of renewal. The past was your teacher, the present is your opportunity, and the future is your reward."
        ),
        TarotReadingResponse(
            past    = "In the past you gave more than you received, and it drained your resources. Yet every drop of energy offered was a seed planted in the soil — soon it will sprout.",
            present = "The time has come to care for yourself. Set your priorities and don't be afraid to say 'no' to what doesn't serve you. Your energy is your greatest resource.",
            future  = "A period of abundance and harmony lies ahead. Relationships will deepen, projects will gain momentum, and inner peace will become your constant companion.",
            summary = "The spread points to a cycle of giving and receiving that is finally returning to balance. The universe does not forget those who act with an open heart."
        ),
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
        get() = when (LanguageManager.current) {
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
            val reading = try {
                val mock = (MOCK_READINGS[lang] ?: MOCK_READINGS["en"]!!).random()
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
                (MOCK_READINGS[lang] ?: MOCK_READINGS["en"]!!).random()
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
        // Реклама недоступна — всё равно делаем расклад, чтобы не блокировать пользователя
        drawCards()
    }

    private suspend fun revealCardsWithDelay() {
        repeat(3) { i ->
            kotlinx.coroutines.delay(550L)
            _state.value = _state.value.copy(revealedCount = i + 1)
        }
    }
}

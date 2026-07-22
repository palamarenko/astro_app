package com.iruna.app.data

import kotlinx.serialization.Serializable

/**
 * Снимок последнего расклада. Используется чтобы:
 *  - запомнить выбранные карты (по римскому номеру + перевернутость)
 *  - запомнить уже полученное от ИИ описание
 *  - не давать пользователю сделать новый расклад в тот же день
 */
@Serializable
data class TarotPersistState(
    val date: String,                                  // YYYY-MM-DD (локальный день)
    val cards: List<TarotCardSnapshot>,
    val reading: TarotReadingResponse,
)

@Serializable
data class TarotCardSnapshot(
    val number: String,                                // римская цифра (ключ карты)
    val reversed: Boolean,
)

/**
 * Платформенно-зависимое хранилище состояния раскладов.
 *  - Android: SharedPreferences
 *  - iOS:     NSUserDefaults
 *
 *  Ключи периодов: "daily" | "weekly" | "monthly"
 */
expect object TarotStorage {
    /** Ключ сегодняшнего дня в формате YYYY-MM-DD */
    fun todayKey(): String

    /** Ключ текущей недели в формате YYYY-Www (напр. 2026-W19) */
    fun weekKey(): String

    /** Ключ текущего месяца в формате YYYY-MM */
    fun monthKey(): String

    /** Загрузить снимок для заданного периода или null. */
    fun loadPeriod(period: String): TarotPersistState?

    /** Сохранить снимок для заданного периода. */
    fun savePeriod(period: String, state: TarotPersistState)

    /** Загрузить снимок последнего расклада (legacy). */
    fun load(): TarotPersistState?

    /** Сохранить снимок (legacy). */
    fun save(state: TarotPersistState)

    /** Дата (YYYY-MM-DD), для которой уже проигрывалась анимация раскрытия
     *  «Карты дня», или null. Используется, чтобы флип показывался только
     *  при первом открытии карты в этот день. */
    fun dayCardRevealedDate(): String?

    /** Запоминает дату, для которой анимация раскрытия «Карты дня» показана. */
    fun setDayCardRevealedDate(date: String)

    /** Очистить все сохранённые расклады. */
    fun clear()
}

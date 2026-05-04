package com.astro.app.data

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
 * Платформенно-зависимое хранилище состояния "колоды дня".
 *  - Android: SharedPreferences
 *  - iOS:     NSUserDefaults
 */
expect object TarotStorage {
    /** Ключ сегодняшнего дня в формате YYYY-MM-DD */
    fun todayKey(): String

    /** Загрузить снимок последнего расклада или null. */
    fun load(): TarotPersistState?

    /** Сохранить снимок. */
    fun save(state: TarotPersistState)

    /** Очистить состояние (например, для отладки). */
    fun clear()
}

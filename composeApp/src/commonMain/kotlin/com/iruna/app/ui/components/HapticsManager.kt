package com.iruna.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Централизованное состояние тактильного отклика (вибрации).
 *
 * - [init]    — вызвать один раз при старте (после UserStorage.load()).
 * - [enabled] — реактивное значение для чтения в composable и в хелпере вибрации.
 * - [set]     — переключить (профиль сохраняет значение отдельно, через ViewModel).
 */
object HapticsManager {
    /** Compose-состояние: перерисовывает подписчиков при изменении. */
    var enabled by mutableStateOf(true)
        private set

    fun init(value: Boolean) {
        enabled = value
    }

    fun set(value: Boolean) {
        enabled = value
    }
}

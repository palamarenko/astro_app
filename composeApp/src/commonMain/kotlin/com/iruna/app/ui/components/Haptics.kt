package com.iruna.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * Платформенный «тик» вибромотора (Android — Vibrator, iOS — UIImpactFeedbackGenerator).
 * Без учёта пользовательской настройки — гейт делается в [rememberSelectionHaptic].
 */
@Composable
expect fun rememberPlatformHaptic(): () -> Unit

/**
 * Лёгкий тактильный отклик для навигации: листание знаков, выбор периода,
 * нажатия в таро и т.п. Уважает переключатель [HapticsManager.enabled].
 */
@Composable
fun rememberSelectionHaptic(): () -> Unit {
    val platform = rememberPlatformHaptic()
    return remember(platform) {
        { if (HapticsManager.enabled) platform() }
    }
}

/**
 * «Рамбл» на время анимации: повторяет короткие тики, пока идёт эффект
 * (например, переворот карты таро). Вызывать из корутины/LaunchedEffect.
 *
 * @param durationMs общая длительность вибрации.
 * @param stepMs     интервал между тиками.
 */
suspend fun runHapticRumble(
    tick: () -> Unit,
    durationMs: Long,
    stepMs: Long = 70L,
) {
    if (!HapticsManager.enabled) return
    var elapsed = 0L
    while (elapsed < durationMs) {
        tick()
        delay(stepMs)
        elapsed += stepMs
    }
}

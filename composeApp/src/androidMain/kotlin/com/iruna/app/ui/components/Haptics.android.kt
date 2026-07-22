package com.iruna.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformHaptic(): () -> Unit {
    val context = LocalContext.current
    val vibrator = remember(context) { context.selectionVibrator() }
    return remember(vibrator) {
        { vibrator?.tick() }
    }
}

private fun Context.selectionVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

// Мягкий «тик».
private const val TICK_MS = 22L
private const val TICK_AMPLITUDE = 180 // 1..255; ниже 255 — деликатнее

// USAGE_ALARM: такие вибрации не гасятся системным тумблером «тактильная
// отдача» — на ряде прошивок (в т.ч. vivo) иначе отклик не срабатывает.
private val alarmVibrationAttributes: VibrationAttributes? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_ALARM)
            .build()
    } else null

@Suppress("DEPRECATION")
private val alarmAudioAttributes: AudioAttributes =
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

private fun Vibrator.tick() {
    if (!hasVibrator()) return
    val amplitude = if (hasAmplitudeControl()) TICK_AMPLITUDE else VibrationEffect.DEFAULT_AMPLITUDE
    try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                vibrate(
                    VibrationEffect.createOneShot(TICK_MS, amplitude),
                    alarmVibrationAttributes!!,
                )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                @Suppress("DEPRECATION")
                vibrate(
                    VibrationEffect.createOneShot(TICK_MS, amplitude),
                    alarmAudioAttributes,
                )

            else -> {
                @Suppress("DEPRECATION")
                vibrate(TICK_MS)
            }
        }
    } catch (_: Throwable) {
        // Вибрация не критична — молча игнорируем сбои.
    }
}

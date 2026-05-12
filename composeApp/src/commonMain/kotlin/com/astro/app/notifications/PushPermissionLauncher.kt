package com.astro.app.notifications

import androidx.compose.runtime.Composable

/**
 * Возвращает функцию, которую нужно вызвать, чтобы запросить разрешение
 * на показ уведомлений.
 *
 * Android: показывает системный диалог POST_NOTIFICATIONS (Android 13+).
 * iOS:     no-op (разрешение запрашивается отдельно через iOS API).
 *
 * @param onResult  true — пользователь разрешил, false — отказал
 */
@Composable
expect fun rememberPushPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit

package com.iruna.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Перехватывает системную кнопку «Назад».
 * Android : androidx.activity.compose.BackHandler
 * iOS     : нет системной кнопки — no-op.
 */
@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)

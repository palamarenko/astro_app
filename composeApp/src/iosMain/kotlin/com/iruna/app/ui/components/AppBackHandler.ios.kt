package com.iruna.app.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS не имеет системной кнопки «Назад» — no-op
}

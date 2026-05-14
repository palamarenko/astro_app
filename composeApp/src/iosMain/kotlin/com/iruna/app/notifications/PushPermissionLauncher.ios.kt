package com.iruna.app.notifications

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPushPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    // iOS — push-разрешение обрабатывается нативно через AppDelegate,
    // здесь просто no-op
    return {}
}

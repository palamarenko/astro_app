package com.iruna.app.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberPushPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    // На Android < 13 разрешение не нужно — сразу сообщаем, что выдано
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return { onResult(true) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )
    return { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

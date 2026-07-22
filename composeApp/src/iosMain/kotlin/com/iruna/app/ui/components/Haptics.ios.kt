package com.iruna.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyleLight

@Composable
actual fun rememberPlatformHaptic(): () -> Unit {
    val generator = remember {
        UIImpactFeedbackGenerator(UIImpactFeedbackStyleLight).apply { prepare() }
    }
    return remember(generator) {
        {
            generator.impactOccurred()
            generator.prepare()
        }
    }
}

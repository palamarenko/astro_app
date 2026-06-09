package com.iruna.app.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun rememberAdManager(): AdManager {
    LaunchedEffect(Unit) { IosAdBridge.preload?.invoke() }
    return IosAdManagerImpl
}

private object IosAdManagerImpl : AdManager {
    override val isAdReady: Boolean
        get() = IosAdBridge.adReady

    override fun preloadAd() {
        IosAdBridge.preload?.invoke()
    }

    override fun showRewardedAd(onRewarded: () -> Unit, onFailed: () -> Unit) {
        val show = IosAdBridge.showAd
        if (show != null) {
            show(onRewarded, onFailed)
        } else {
            onFailed()
        }
    }
}

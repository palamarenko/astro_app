package com.iruna.app.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun rememberAdManager(): AdManager {
    LaunchedEffect(Unit) { IosAdBridge.delegate?.preload() }
    return IosAdManagerImpl
}

private object IosAdManagerImpl : AdManager {
    override val isAdReady: Boolean
        get() = IosAdBridge.delegate?.isAdReady ?: false

    override fun preloadAd() {
        IosAdBridge.delegate?.preload()
    }

    override fun showRewardedAd(onRewarded: () -> Unit, onFailed: () -> Unit) {
        val delegate = IosAdBridge.delegate
        if (delegate != null) {
            delegate.showAd(object : AdRewardCallback {
                override fun onRewarded() = onRewarded()
                override fun onFailed() = onFailed()
            })
        } else {
            onFailed()
        }
    }
}

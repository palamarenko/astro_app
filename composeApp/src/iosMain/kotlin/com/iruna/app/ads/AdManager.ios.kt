package com.iruna.app.ads

import androidx.compose.runtime.Composable

/**
 * iOS-заглушка: AdMob для iOS требует отдельной настройки через CocoaPods/SPM.
 * TODO: Подключить Google-Mobile-Ads-SDK через Podfile и реализовать показ рекламы.
 */
@Composable
actual fun rememberAdManager(): AdManager = IosAdManager

private object IosAdManager : AdManager {
    override val isAdReady: Boolean = false
    override fun preloadAd() = Unit
    override fun showRewardedAd(onRewarded: () -> Unit, onFailed: () -> Unit) = onFailed()
}

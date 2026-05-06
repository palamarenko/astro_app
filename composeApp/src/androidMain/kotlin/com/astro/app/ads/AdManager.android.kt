package com.astro.app.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.astro.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Ad Unit ID берётся из local.properties → BuildConfig.ADMOB_REWARDED_AD_UNIT_ID */
private val REWARDED_AD_UNIT_ID get() = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID

@Composable
actual fun rememberAdManager(): AdManager {
    val activity = LocalContext.current as Activity
    val manager = remember(activity) { AndroidAdManager(activity) }
    LaunchedEffect(manager) { manager.preloadAd() }
    return manager
}

internal class AndroidAdManager(private val activity: Activity) : AdManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    override val isAdReady: Boolean
        get() = rewardedAd != null

    override fun preloadAd() {
        if (isLoading || isAdReady) return
        isLoading = true
        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    override fun showRewardedAd(onRewarded: () -> Unit, onFailed: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onFailed()
            preloadAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadAd() // предзагружаем следующую
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onFailed()
                preloadAd()
            }
        }

        ad.show(activity) { /* RewardItem — факт показа достаточен */ onRewarded() }
    }
}

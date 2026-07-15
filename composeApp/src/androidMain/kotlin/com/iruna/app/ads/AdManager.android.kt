package com.iruna.app.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.iruna.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** Ad Unit ID берётся из local.properties → BuildConfig.ADMOB_REWARDED_AD_UNIT_ID */
private val REWARDED_AD_UNIT_ID get() = BuildConfig.ADMOB_REWARDED_AD_UNIT_ID

/** Разворачивает цепочку ContextWrapper, пока не найдёт Activity. */
private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in context chain: $this")
}

@Composable
actual fun rememberAdManager(): AdManager {
    val activity = LocalContext.current.findActivity()
    val manager = remember(activity) { AndroidAdManager(activity) }
    LaunchedEffect(manager) { manager.preloadAd() }
    return manager
}

internal class AndroidAdManager(private val activity: Activity) : AdManager {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /** Номер текущей попытки загрузки — для экспоненциального backoff. */
    private var retryAttempt = 0
    /** Запланированный ретрай (чтобы не плодить дубли и уметь отменять). */
    private var pendingRetry: Runnable? = null

    private val handler = Handler(Looper.getMainLooper())

    override val isAdReady: Boolean
        get() = rewardedAd != null

    override fun preloadAd() {
        // Уже есть готовая реклама или идёт загрузка — ничего не делаем.
        if (isLoading || isAdReady) return
        // Отменяем возможный отложенный ретрай — грузим прямо сейчас.
        cancelPendingRetry()
        isLoading = true

        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    retryAttempt = 0 // успех — сбрасываем backoff
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    // Ключевое отличие: не сдаёмся, а повторяем с нарастающей паузой,
                    // чтобы isAdReady как можно быстрее снова стал true и показы не утекали.
                    scheduleRetry()
                }
            }
        )
    }

    /** Планирует повторную загрузку с экспоненциальным backoff: 2, 4, 8, 16, 32, 64 сек. */
    private fun scheduleRetry() {
        if (pendingRetry != null) return // ретрай уже запланирован
        val delayMs = RETRY_BASE_DELAY_MS shl retryAttempt.coerceAtMost(MAX_RETRY_SHIFT)
        if (retryAttempt < MAX_RETRY_SHIFT) retryAttempt++
        val task = Runnable {
            pendingRetry = null
            preloadAd()
        }
        pendingRetry = task
        handler.postDelayed(task, delayMs)
    }

    private fun cancelPendingRetry() {
        pendingRetry?.let { handler.removeCallbacks(it) }
        pendingRetry = null
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
                retryAttempt = 0        // следующий цикл — с чистого листа
                preloadAd()             // сразу предзагружаем следующую
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onFailed()
                preloadAd()
            }
        }

        ad.show(activity) { /* RewardItem — факт показа достаточен */ onRewarded() }
    }

    private companion object {
        /** Базовая задержка ретрая (1-я пауза = 2 сек). */
        const val RETRY_BASE_DELAY_MS = 2_000L
        /** Максимальный сдвиг: 2 сек << 5 = 64 сек — потолок паузы. */
        const val MAX_RETRY_SHIFT = 5
    }
}

package com.iruna.app.ads

import androidx.compose.runtime.Composable

/**
 * Платформо-независимый интерфейс для управления рекламой AdMob.
 */
interface AdManager {
    /** true — rewarded-реклама загружена и готова к показу. */
    val isAdReady: Boolean

    /** Предзагрузить следующую rewarded-рекламу. */
    fun preloadAd()

    /**
     * Показать rewarded-рекламу.
     * [onRewarded] — вызывается после того как пользователь досмотрел рекламу.
     * [onFailed]   — вызывается если реклама не готова или произошла ошибка.
     */
    fun showRewardedAd(onRewarded: () -> Unit, onFailed: () -> Unit)
}

/** Возвращает платформо-специфичный [AdManager], привязанный к Compose lifecycle. */
@Composable
expect fun rememberAdManager(): AdManager

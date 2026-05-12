package com.astro.app.notifications

/**
 * Показывает тестовое уведомление локально на устройстве, минуя FCM.
 * Android: вызывает NotificationHelper напрямую.
 * iOS: no-op (admin panel на iOS не используется).
 */
expect fun sendLocalTestPush(signName: String)

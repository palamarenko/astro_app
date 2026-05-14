package com.iruna.app.notifications

import com.iruna.app.data.TarotStorageInitializer

actual fun sendLocalTestPush(signName: String) {
    val ctx = TarotStorageInitializer.appContext ?: return
    NotificationHelper.createChannel(ctx)
    NotificationHelper.showDailyHoroscope(
        context  = ctx,
        signName = signName,
        notifId  = 9999,  // отдельный ID чтобы не конфликтовал с реальными
    )
}

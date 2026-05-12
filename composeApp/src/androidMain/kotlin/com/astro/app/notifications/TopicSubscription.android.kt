package com.astro.app.notifications

actual fun subscribeToPushTopic() {
    PushNotificationService.subscribeToTopic()
}

package com.iruna.app.notifications

actual fun subscribeToPushTopic() {
    PushNotificationService.subscribeToTopic()
}

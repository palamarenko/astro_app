package com.iruna.app.data

/**
 * iOS-заглушка. Нативный Firebase SDK на iOS пока не подключён
 * (в Podfile только Google-Mobile-Ads-SDK, GoogleService-Info.plist отсутствует).
 *
 * Чтобы включить аналитику на iOS:
 *   1. Добавить `pod 'FirebaseAnalytics'` в iosApp/Podfile и `pod install`.
 *   2. Положить GoogleService-Info.plist в проект iosApp.
 *   3. Вызвать `FirebaseApp.configure()` в AppDelegate.
 *   4. Реализовать методы ниже через FIRAnalytics (cinterop/CocoaPods).
 */
actual object Analytics {
    actual fun log(event: String, params: Map<String, Any?>) {
        // no-op до подключения нативного Firebase на iOS
    }

    actual fun setUserProperty(name: String, value: String?) {
        // no-op
    }

    actual fun setUserId(id: String?) {
        // no-op
    }
}

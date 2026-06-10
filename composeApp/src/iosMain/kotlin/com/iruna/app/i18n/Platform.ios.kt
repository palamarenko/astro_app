package com.iruna.app.i18n

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

actual fun getSystemLanguageCode(): String {
    // preferredLanguages даёт реальный язык интерфейса пользователя,
    // например "ru-RU", "uk-UA", "en-US" — берём подтег языка до "-".
    val preferred = (NSLocale.preferredLanguages.firstOrNull() as? String)
        ?.substringBefore("-")
        ?.substringBefore("_")

    return preferred?.takeIf { it.isNotEmpty() } ?: NSLocale.currentLocale.languageCode
}

actual fun applyLanguage(code: String) {
    // Записываем предпочтительный язык — CMP ресурсы подхватят при следующей
    // полной перекомпозиции (key(lang) в App.kt).
    NSUserDefaults.standardUserDefaults.setObject(listOf(code), forKey = "AppleLanguages")
    NSUserDefaults.standardUserDefaults.synchronize()
}

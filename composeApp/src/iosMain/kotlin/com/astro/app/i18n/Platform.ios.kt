package com.astro.app.i18n

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getSystemLanguageCode(): String = NSLocale.currentLocale.languageCode

actual fun applyLanguage(code: String) {
    // Записываем предпочтительный язык — CMP ресурсы подхватят при следующей
    // полной перекомпозиции (key(lang) в App.kt).
    NSUserDefaults.standardUserDefaults.setObject(listOf(code), forKey = "AppleLanguages")
    NSUserDefaults.standardUserDefaults.synchronize()
}

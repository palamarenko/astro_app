package com.astro.app.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

actual fun getSystemLanguageCode(): String = Locale.getDefault().language

actual fun applyLanguage(code: String) {
    // API 33+: работает без перезапуска Activity.
    // Ниже API 33: AppCompat автоматически воссоздаёт Activity.
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(code)
    )
}

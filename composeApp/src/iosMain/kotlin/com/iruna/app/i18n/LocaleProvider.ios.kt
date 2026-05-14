package com.iruna.app.i18n

import androidx.compose.runtime.Composable

@Composable
actual fun ProvideLocale(lang: AppLanguage, content: @Composable () -> Unit) {
    // iOS: NSUserDefaults["AppleLanguages"] уже обновлён в applyLanguage().
    // NSLocale.preferredLanguages читает из NSUserDefaults мгновенно,
    // поэтому key(lang) в App() заставит CMP перечитать ресурсы
    // с нужным языком без дополнительных обёрток.
    content()
}

package com.iruna.app.i18n

/** Возвращает ISO-код языка устройства (например "ru", "uk", "en"). */
expect fun getSystemLanguageCode(): String

/**
 * Применяет язык [code] на платформенном уровне.
 * Android : обновляет AppCompatDelegate locale (API 33+ — без перезапуска).
 * iOS     : записывает AppleLanguages в NSUserDefaults.
 */
expect fun applyLanguage(code: String)

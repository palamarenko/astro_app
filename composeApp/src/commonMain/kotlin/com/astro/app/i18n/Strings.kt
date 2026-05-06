package com.astro.app.i18n

enum class AppLanguage(val code: String) {
    RU("ru"), UK("uk"), EN("en");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code.lowercase().take(2) } ?: RU
    }
}

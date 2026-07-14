package com.iruna.app.i18n

/**
 * Список поддерживаемых языков приложения.
 *
 * ── Как добавить новый язык ───────────────────────────────────────────────────
 * 1. Добавить новую запись в этот enum (code, nativeName).
 * 2. Создать папку composeResources/values-<code>/strings.xml с переводами.
 * 3. Больше ничего менять не нужно — LanguageManager подхватит автоматически.
 * ─────────────────────────────────────────────────────────────────────────────
 */
enum class AppLanguage(
    val code: String,
    /** Название на родном языке — всегда отображается без перевода. */
    val nativeName: String,
    /** Язык с письмом справа налево (например, арабский). */
    val isRtl: Boolean = false,
) {
    EN("en", "English"),
    ES("es", "Español"),
    DE("de", "Deutsch"),
    FR("fr", "Français"),
    RU("ru", "Русский"),
    UK("uk", "Українська"),
    AR("ar", "العربية", isRtl = true);

    companion object {
        /** Найти язык по ISO-коду (первые 2 символа). По умолчанию — EN. */
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code.lowercase().take(2) } ?: EN

        /** Язык устройства → ближайший поддерживаемый, иначе EN. */
        fun deviceDefault(): AppLanguage = fromCode(getSystemLanguageCode())
    }
}

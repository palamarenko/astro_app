package com.iruna.app.data

import kotlinx.serialization.Serializable

/**
 * Профиль пользователя, сохраняемый локально.
 * Восстанавливается при каждом запуске приложения.
 */
@Serializable
data class UserProfile(
    val signId: String = "leo",
    val name: String = "",
    val gender: String = "",        // "male" | "female" | ""
    val birthDay: Int = 0,
    val birthMonth: Int = 0,
    val birthYear: Int = 1990,
    val birthHour: Int = -1,
    val birthMinute: Int = 0,
    val birthPlace: String = "",
    val birthLat: Double = 0.0,
    val birthLng: Double = 0.0,

    // ── Language ──────────────────────────────────────────────────────────────
    /** ISO-код выбранного языка ("ru", "uk", "en"). Пустая строка = язык устройства. */
    val language: String = "",

    // ── Onboarding state ─────────────────────────────────────────────────────
    /** Последний просмотренный шаг онбординга (0..N). Используется для возобновления. */
    val onboardingStep: Int = 0,
    /** true — онбординг завершён (или явно пропущен). Больше не показываем. */
    val onboardingFinished: Boolean = false,

    // ── Push Notifications ────────────────────────────────────────────────────
    /** true — уже показывали диалог с запросом разрешения. */
    val pushNotificationsAsked: Boolean = false,
    /** true — пользователь разрешил уведомления. */
    val pushNotificationsEnabled: Boolean = false,

    // ── Haptics ───────────────────────────────────────────────────────────────
    /** true — тактильный отклик (вибрация) включён. */
    val hapticsEnabled: Boolean = true,

    // ── Admin settings ────────────────────────────────────────────────────────
    val adminFunctionUrl: String = "",
    val adminSecret: String = "",
)

/**
 * Платформенно-зависимое хранилище профиля.
 *  - Android: SharedPreferences
 *  - iOS:     NSUserDefaults
 */
expect object UserStorage {
    fun load(): UserProfile?
    fun save(profile: UserProfile)
    fun clear()
}

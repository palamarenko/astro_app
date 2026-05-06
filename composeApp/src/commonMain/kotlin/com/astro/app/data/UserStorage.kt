package com.astro.app.data

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

    // ── Onboarding state ─────────────────────────────────────────────────────
    /** Последний просмотренный шаг онбординга (0..N). Используется для возобновления. */
    val onboardingStep: Int = 0,
    /** true — онбординг завершён (или явно пропущен). Больше не показываем. */
    val onboardingFinished: Boolean = false,
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

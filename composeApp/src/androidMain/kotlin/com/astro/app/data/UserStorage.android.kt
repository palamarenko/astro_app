package com.astro.app.data

import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual object UserStorage {
    private const val PREFS = "user_prefs"
    private const val KEY   = "profile_json"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prefs(): SharedPreferences? =
        TarotStorageInitializer.appContext?.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    actual fun load(): UserProfile? {
        val raw = prefs()?.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString<UserProfile>(raw) }.getOrNull()
    }

    actual fun save(profile: UserProfile) {
        prefs()?.edit()?.putString(KEY, json.encodeToString(profile))?.apply()
    }

    actual fun clear() {
        prefs()?.edit()?.remove(KEY)?.apply()
    }
}

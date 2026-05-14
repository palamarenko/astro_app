package com.iruna.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

actual object UserStorage {
    private const val KEY = "user_profile_json"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    actual fun load(): UserProfile? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(KEY) ?: return null
        return runCatching { json.decodeFromString<UserProfile>(raw) }.getOrNull()
    }

    actual fun save(profile: UserProfile) {
        NSUserDefaults.standardUserDefaults.setObject(json.encodeToString(profile), KEY)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY)
    }
}

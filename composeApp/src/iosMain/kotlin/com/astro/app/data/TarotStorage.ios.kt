package com.astro.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSUserDefaults
import platform.Foundation.localTimeZone
import platform.Foundation.localeWithLocaleIdentifier

actual object TarotStorage {
    private const val KEY_STATE = "tarot_state_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val formatter: NSDateFormatter by lazy {
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
            timeZone = NSTimeZone.localTimeZone
        }
    }

    actual fun todayKey(): String = formatter.stringFromDate(NSDate())

    actual fun load(): TarotPersistState? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(KEY_STATE) ?: return null
        return runCatching { json.decodeFromString<TarotPersistState>(raw) }.getOrNull()
    }

    actual fun save(state: TarotPersistState) {
        NSUserDefaults.standardUserDefaults.setObject(json.encodeToString(state), KEY_STATE)
    }

    actual fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_STATE)
    }
}

package com.iruna.app.data

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
    private const val KEY_DAYCARD_REVEALED = "tarot_daycard_revealed_date"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dayFormatter: NSDateFormatter by lazy {
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
            timeZone = NSTimeZone.localTimeZone
        }
    }
    private val weekFormatter: NSDateFormatter by lazy {
        NSDateFormatter().apply {
            dateFormat = "yyyy-'W'ww"
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
            timeZone = NSTimeZone.localTimeZone
        }
    }
    private val monthFormatter: NSDateFormatter by lazy {
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM"
            locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
            timeZone = NSTimeZone.localTimeZone
        }
    }

    actual fun todayKey(): String = dayFormatter.stringFromDate(NSDate())
    actual fun weekKey(): String  = weekFormatter.stringFromDate(NSDate())
    actual fun monthKey(): String = monthFormatter.stringFromDate(NSDate())

    private fun periodNsKey(period: String) = "tarot_state_${period}_json"

    actual fun loadPeriod(period: String): TarotPersistState? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(periodNsKey(period)) ?: return null
        return runCatching { json.decodeFromString<TarotPersistState>(raw) }.getOrNull()
    }

    actual fun savePeriod(period: String, state: TarotPersistState) {
        NSUserDefaults.standardUserDefaults.setObject(json.encodeToString(state), periodNsKey(period))
    }

    actual fun load(): TarotPersistState? {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(KEY_STATE) ?: return null
        return runCatching { json.decodeFromString<TarotPersistState>(raw) }.getOrNull()
    }

    actual fun save(state: TarotPersistState) {
        NSUserDefaults.standardUserDefaults.setObject(json.encodeToString(state), KEY_STATE)
    }

    actual fun dayCardRevealedDate(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_DAYCARD_REVEALED)

    actual fun setDayCardRevealedDate(date: String) {
        NSUserDefaults.standardUserDefaults.setObject(date, KEY_DAYCARD_REVEALED)
    }

    actual fun clear() {
        listOf(KEY_STATE, "daily", "weekly", "monthly").forEach {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(
                if (it == KEY_STATE) it else periodNsKey(it)
            )
        }
    }
}

package com.astro.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Android-реализация хранилища.
 * Перед использованием необходимо вызвать [TarotStorageInitializer.init] из Application/Activity.
 */
@SuppressLint("StaticFieldLeak")
object TarotStorageInitializer {
    internal var appContext: Context? = null
    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

actual object TarotStorage {
    private const val PREFS = "tarot_prefs"
    private const val KEY_STATE = "state_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getDefault()
    }
    private val isoWeek = SimpleDateFormat("yyyy-'W'ww", Locale.ROOT).apply {
        timeZone = TimeZone.getDefault()
    }
    private val isoMonth = SimpleDateFormat("yyyy-MM", Locale.ROOT).apply {
        timeZone = TimeZone.getDefault()
    }

    private fun prefs(): SharedPreferences? =
        TarotStorageInitializer.appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun todayKey(): String = isoDate.format(Date())
    actual fun weekKey(): String  = isoWeek.format(Date())
    actual fun monthKey(): String = isoMonth.format(Date())

    private fun periodKey(period: String) = "state_${period}_json"

    actual fun loadPeriod(period: String): TarotPersistState? {
        val raw = prefs()?.getString(periodKey(period), null) ?: return null
        return runCatching { json.decodeFromString<TarotPersistState>(raw) }.getOrNull()
    }

    actual fun savePeriod(period: String, state: TarotPersistState) {
        prefs()?.edit()?.putString(periodKey(period), json.encodeToString(state))?.apply()
    }

    actual fun load(): TarotPersistState? {
        val raw = prefs()?.getString(KEY_STATE, null) ?: return null
        return runCatching { json.decodeFromString<TarotPersistState>(raw) }.getOrNull()
    }

    actual fun save(state: TarotPersistState) {
        prefs()?.edit()?.putString(KEY_STATE, json.encodeToString(state))?.apply()
    }

    actual fun clear() {
        prefs()?.edit()?.clear()?.apply()
    }
}

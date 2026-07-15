package com.iruna.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Инициализатор — вызвать из [MainActivity.onCreate] один раз (после Firebase auto-init).
 * Аналог [TarotStorageInitializer].
 */
@SuppressLint("StaticFieldLeak")
object AnalyticsInitializer {
    internal var firebase: FirebaseAnalytics? = null
    fun init(context: Context) {
        firebase = FirebaseAnalytics.getInstance(context.applicationContext)
    }
}

actual object Analytics {
    private val fa: FirebaseAnalytics? get() = AnalyticsInitializer.firebase

    actual fun log(event: String, params: Map<String, Any?>) {
        val fa = fa ?: return
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    null -> {}
                    is String -> putString(key, value)
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putDouble(key, value.toDouble())
                    is Boolean -> putString(key, value.toString())
                    else -> putString(key, value.toString())
                }
            }
        }
        fa.logEvent(event, bundle)
    }

    actual fun setUserProperty(name: String, value: String?) {
        fa?.setUserProperty(name, value)
    }

    actual fun setUserId(id: String?) {
        fa?.setUserId(id)
    }
}

package com.iruna.app.i18n

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale

@Composable
actual fun ProvideLocale(lang: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current

    // Ключ включает и context: если Activity пересоздаётся после возврата из
    // рекламного экрана — Compose даёт новый LocalContext, и мы пересоздаём
    // localizedContext уже на основе нового Activity-контекста.
    val localizedContext = remember(lang, context) {
        val locale = Locale(lang.code)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        // Это критично: обновляем текущие ресурсы Activity,
        // чтобы системные диалоги и внешние компоненты видели изменения
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val localizedResources = context.createConfigurationContext(config).resources
        object : ContextWrapper(context) {
            override fun getResources(): Resources = localizedResources
        }
    }

    // При возврате из рекламного экрана (onResume) принудительно переприменяем
    // Locale.setDefault() — некоторые сторонние SDK его сбрасывают.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, lang) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Locale.setDefault(Locale(lang.code))
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        content()
    }
}

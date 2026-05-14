package com.iruna.app.i18n

import androidx.compose.runtime.Composable

/**
 * Оборачивает контент так, чтобы все вызовы stringResource() внутри
 * читали строки на языке [lang].
 *
 * Android : предоставляет LocalContext с нужной Configuration.locale →
 *           строки переключаются немедленно без перезапуска Activity.
 * iOS     : no-op (NSUserDefaults уже записан в applyLanguage;
 *           CMP перечитывает NSLocale.preferredLanguages при key(lang)).
 */
@Composable
expect fun ProvideLocale(lang: AppLanguage, content: @Composable () -> Unit)

package com.astro.app.i18n

import java.util.Locale

actual fun getSystemLanguageCode(): String = Locale.getDefault().language

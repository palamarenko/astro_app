package com.astro.app.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Централизованный менеджер языка приложения.
 *
 * Использование:
 *   - [init]        — вызвать один раз при старте (после инициализации UserStorage).
 *   - [language]    — StateFlow для наблюдения текущего языка в UI.
 *   - [setLanguage] — изменить язык (сохраняет в UserStorage через колбэк).
 *   - [current]     — синхронный геттер для ViewModel.
 *
 * Для добавления нового языка — только правка [AppLanguage] enum + strings.xml.
 */
object LanguageManager {

    private val _language = MutableStateFlow(AppLanguage.EN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    /** Текущий язык (синхронный доступ для ViewModel). */
    val current: AppLanguage get() = _language.value

    /**
     * Инициализация. Вызывать после UserStorage.load().
     * @param savedCode  сохранённый ISO-код или null/пусто → язык устройства.
     */
    fun init(savedCode: String?) {
        val lang = if (!savedCode.isNullOrEmpty()) {
            AppLanguage.fromCode(savedCode)
        } else {
            AppLanguage.deviceDefault()
        }
        _language.value = lang
        applyLanguage(lang.code)
    }

    /**
     * Сменить язык. [onSave] сохраняет выбор в постоянное хранилище.
     */
    fun setLanguage(lang: AppLanguage, onSave: (String) -> Unit) {
        _language.value = lang
        applyLanguage(lang.code)
        onSave(lang.code)
    }
}

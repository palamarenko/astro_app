package com.iruna.app.data

/**
 * Платформо-независимая обёртка над аналитикой (Firebase Analytics на Android,
 * заглушка на iOS до подключения нативного Firebase SDK).
 *
 * Низкоуровневый API — [log], [setUserProperty], [setUserId].
 * Для экранов используйте типизированные хелперы из [Track].
 */
expect object Analytics {
    /** Отправить событие. Значения params — String / Int / Long / Double / Boolean. */
    fun log(event: String, params: Map<String, Any?> = emptyMap())

    /** Задать свойство пользователя (для сегментации). */
    fun setUserProperty(name: String, value: String?)

    /** Задать идентификатор пользователя (или null, чтобы сбросить). */
    fun setUserId(id: String?)
}

/** Имена событий (snake_case, латиница). */
object AnalyticsEvent {
    // Жизненный цикл
    const val SCREEN_VIEW = "screen_view"
    const val APP_OPEN = "app_open"

    // Онбординг
    const val ONBOARDING_START = "onboarding_start"
    const val ONBOARDING_STEP_COMPLETE = "onboarding_step_complete"
    const val ONBOARDING_SKIP = "onboarding_skip"
    const val ONBOARDING_COMPLETE = "onboarding_complete"

    // Гороскоп
    const val HOROSCOPE_SIGN_SELECT = "horoscope_sign_select"
    const val HOROSCOPE_PERIOD_SELECT = "horoscope_period_select"
    const val HOROSCOPE_VIEW = "horoscope_view"
    const val HOROSCOPE_WIZARD_CTA_CLICK = "horoscope_wizard_cta_click"
    const val HOROSCOPE_WIZARD_GENERATED = "horoscope_wizard_generated"

    // Таро
    const val TAROT_PERIOD_SELECT = "tarot_period_select"
    const val TAROT_DRAW = "tarot_draw"
    const val TAROT_WIZARD_CTA_CLICK = "tarot_wizard_cta_click"
    const val TAROT_READING_GENERATED = "tarot_reading_generated"

    // Сны
    const val DREAM_DECODE_CLICK = "dream_decode_click"
    const val DREAM_RESULT_VIEW = "dream_result_view"
    const val DREAM_NEW = "dream_new"

    // Совместимость
    const val COMPATIBILITY_OPEN = "compatibility_open"

    // Профиль / настройки
    const val PROFILE_EDIT = "profile_edit"
    const val LANGUAGE_CHANGE = "language_change"

    // Уведомления
    const val PUSH_PROMPT_SHOWN = "push_prompt_shown"
    const val PUSH_PROMPT_DISMISS = "push_prompt_dismiss"
    const val PUSH_PERMISSION_RESULT = "push_permission_result"
    const val PUSH_TOPIC_SUBSCRIBE = "push_topic_subscribe"

    // Реклама (rewarded)
    const val AD_REWARDED_REQUEST = "ad_rewarded_request"
    const val AD_REWARDED_SHOWN = "ad_rewarded_shown"
    const val AD_REWARDED_COMPLETED = "ad_rewarded_completed"
    const val AD_REWARDED_FAILED = "ad_rewarded_failed"

    // AI-генерация
    const val AI_GENERATION_REQUEST = "ai_generation_request"
    const val AI_GENERATION_SUCCESS = "ai_generation_success"
    const val AI_GENERATION_ERROR = "ai_generation_error"
}

/** Имена параметров событий. */
object AnalyticsParam {
    const val SCREEN_NAME = "screen_name"
    const val SOURCE = "source"
    const val STEP = "step"
    const val SIGN = "sign"
    const val PERIOD = "period"
    const val PLACEMENT = "placement"
    const val FEATURE = "feature"
    const val LANG = "lang"
    const val FIELD = "field"
    const val FROM = "from"
    const val TO = "to"
    const val TOPIC = "topic"
    const val GRANTED = "granted"
    const val REASON = "reason"
    const val LATENCY_MS = "latency_ms"
    const val CARDS_COUNT = "cards_count"
    const val TEXT_LENGTH = "text_length"
    const val HAS_NAME = "has_name"
    const val HAS_BIRTH_TIME = "has_birth_time"
    const val HAS_BIRTH_PLACE = "has_birth_place"
    const val GENDER = "gender"
}

/** Имена свойств пользователя. */
object AnalyticsUserProp {
    const val ZODIAC_SIGN = "zodiac_sign"
    const val GENDER = "gender"
    const val APP_LANGUAGE = "app_language"
    const val HAS_BIRTH_TIME = "has_birth_time"
    const val HAS_BIRTH_PLACE = "has_birth_place"
    const val NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
}

/**
 * Типизированные хелперы — вызывайте их из экранов вместо «сырых» строк.
 * Так имена событий и параметров остаются в одном месте.
 */
object Track {
    private fun bucketTextLength(len: Int): String = when {
        len <= 50 -> "0-50"
        len <= 200 -> "50-200"
        else -> "200+"
    }

    // Жизненный цикл
    fun screen(name: String) =
        Analytics.log(AnalyticsEvent.SCREEN_VIEW, mapOf(AnalyticsParam.SCREEN_NAME to name))

    // Онбординг
    fun onboardingStart() = Analytics.log(AnalyticsEvent.ONBOARDING_START)

    fun onboardingStepComplete(step: String) =
        Analytics.log(AnalyticsEvent.ONBOARDING_STEP_COMPLETE, mapOf(AnalyticsParam.STEP to step))

    fun onboardingSkip(step: String) =
        Analytics.log(AnalyticsEvent.ONBOARDING_SKIP, mapOf(AnalyticsParam.STEP to step))

    fun onboardingComplete(
        hasName: Boolean, gender: String, hasBirthTime: Boolean,
        hasBirthPlace: Boolean, zodiacSign: String,
    ) = Analytics.log(
        AnalyticsEvent.ONBOARDING_COMPLETE,
        mapOf(
            AnalyticsParam.HAS_NAME to hasName,
            AnalyticsParam.GENDER to gender,
            AnalyticsParam.HAS_BIRTH_TIME to hasBirthTime,
            AnalyticsParam.HAS_BIRTH_PLACE to hasBirthPlace,
            AnalyticsParam.SIGN to zodiacSign,
        ),
    )

    // Гороскоп
    fun horoscopeSignSelect(sign: String) =
        Analytics.log(AnalyticsEvent.HOROSCOPE_SIGN_SELECT, mapOf(AnalyticsParam.SIGN to sign))

    fun horoscopePeriodSelect(period: String) =
        Analytics.log(AnalyticsEvent.HOROSCOPE_PERIOD_SELECT, mapOf(AnalyticsParam.PERIOD to period))

    fun horoscopeView(sign: String, period: String) =
        Analytics.log(
            AnalyticsEvent.HOROSCOPE_VIEW,
            mapOf(AnalyticsParam.SIGN to sign, AnalyticsParam.PERIOD to period),
        )

    fun horoscopeWizardCtaClick(sign: String, period: String) =
        Analytics.log(
            AnalyticsEvent.HOROSCOPE_WIZARD_CTA_CLICK,
            mapOf(AnalyticsParam.SIGN to sign, AnalyticsParam.PERIOD to period),
        )

    fun horoscopeWizardGenerated(sign: String, period: String) =
        Analytics.log(
            AnalyticsEvent.HOROSCOPE_WIZARD_GENERATED,
            mapOf(AnalyticsParam.SIGN to sign, AnalyticsParam.PERIOD to period),
        )

    // Таро
    fun tarotPeriodSelect(period: String) =
        Analytics.log(AnalyticsEvent.TAROT_PERIOD_SELECT, mapOf(AnalyticsParam.PERIOD to period))

    fun tarotDraw(period: String) =
        Analytics.log(AnalyticsEvent.TAROT_DRAW, mapOf(AnalyticsParam.PERIOD to period))

    fun tarotWizardCtaClick(period: String) =
        Analytics.log(AnalyticsEvent.TAROT_WIZARD_CTA_CLICK, mapOf(AnalyticsParam.PERIOD to period))

    fun tarotReadingGenerated(period: String, cardsCount: Int) =
        Analytics.log(
            AnalyticsEvent.TAROT_READING_GENERATED,
            mapOf(AnalyticsParam.PERIOD to period, AnalyticsParam.CARDS_COUNT to cardsCount),
        )

    // Сны
    fun dreamDecodeClick(textLength: Int) =
        Analytics.log(
            AnalyticsEvent.DREAM_DECODE_CLICK,
            mapOf(AnalyticsParam.TEXT_LENGTH to bucketTextLength(textLength)),
        )

    fun dreamResultView() = Analytics.log(AnalyticsEvent.DREAM_RESULT_VIEW)

    fun dreamNew() = Analytics.log(AnalyticsEvent.DREAM_NEW)

    // Совместимость
    fun compatibilityOpen() = Analytics.log(AnalyticsEvent.COMPATIBILITY_OPEN)

    // Профиль / настройки
    fun profileEdit(field: String) =
        Analytics.log(AnalyticsEvent.PROFILE_EDIT, mapOf(AnalyticsParam.FIELD to field))

    fun languageChange(from: String, to: String) =
        Analytics.log(
            AnalyticsEvent.LANGUAGE_CHANGE,
            mapOf(AnalyticsParam.FROM to from, AnalyticsParam.TO to to),
        )

    // Уведомления
    fun pushPromptShown() = Analytics.log(AnalyticsEvent.PUSH_PROMPT_SHOWN)

    fun pushPromptDismiss() = Analytics.log(AnalyticsEvent.PUSH_PROMPT_DISMISS)

    fun pushPermissionResult(granted: Boolean) =
        Analytics.log(AnalyticsEvent.PUSH_PERMISSION_RESULT, mapOf(AnalyticsParam.GRANTED to granted))

    fun pushTopicSubscribe(topic: String) =
        Analytics.log(AnalyticsEvent.PUSH_TOPIC_SUBSCRIBE, mapOf(AnalyticsParam.TOPIC to topic))

    // Реклама
    fun adRewardedRequest(placement: String) =
        Analytics.log(AnalyticsEvent.AD_REWARDED_REQUEST, mapOf(AnalyticsParam.PLACEMENT to placement))

    fun adRewardedShown(placement: String) =
        Analytics.log(AnalyticsEvent.AD_REWARDED_SHOWN, mapOf(AnalyticsParam.PLACEMENT to placement))

    fun adRewardedCompleted(placement: String) =
        Analytics.log(AnalyticsEvent.AD_REWARDED_COMPLETED, mapOf(AnalyticsParam.PLACEMENT to placement))

    fun adRewardedFailed(placement: String, reason: String) =
        Analytics.log(
            AnalyticsEvent.AD_REWARDED_FAILED,
            mapOf(AnalyticsParam.PLACEMENT to placement, AnalyticsParam.REASON to reason),
        )

    // AI-генерация
    fun aiGenerationRequest(feature: String, lang: String) =
        Analytics.log(
            AnalyticsEvent.AI_GENERATION_REQUEST,
            mapOf(AnalyticsParam.FEATURE to feature, AnalyticsParam.LANG to lang),
        )

    fun aiGenerationSuccess(feature: String, lang: String, latencyMs: Long) =
        Analytics.log(
            AnalyticsEvent.AI_GENERATION_SUCCESS,
            mapOf(
                AnalyticsParam.FEATURE to feature,
                AnalyticsParam.LANG to lang,
                AnalyticsParam.LATENCY_MS to latencyMs,
            ),
        )

    fun aiGenerationError(feature: String, lang: String, reason: String) =
        Analytics.log(
            AnalyticsEvent.AI_GENERATION_ERROR,
            mapOf(
                AnalyticsParam.FEATURE to feature,
                AnalyticsParam.LANG to lang,
                AnalyticsParam.REASON to reason,
            ),
        )
}

package com.iruna.app.i18n

/** Все UI-строки приложения. Реализуется объектами: StringsRu, StringsEn, StringsUk, StringsEs, StringsDe, StringsFr. */
interface AppStrings {

    // ── Navigation ────────────────────────────────────────────────────────────
    val nav_horoscope: String
    val nav_tarot: String
    val nav_compatibility: String
    val nav_profile: String

    // ── Sign Picker ───────────────────────────────────────────────────────────
    val sign_picker_label: String
    val sign_picker_title1: String
    val sign_picker_title2: String
    val sign_picker_subtitle: String

    // ── Horoscope ─────────────────────────────────────────────────────────────
    val horoscope_back: String
    val horoscope_select_period: String
    val horoscope_prediction_label: String
    /** Format: %s = error message */
    val horoscope_error: String
    val horoscope_tab_today: String
    val horoscope_tab_week: String
    val horoscope_tab_month: String
    val horoscope_score_love: String
    val horoscope_score_career: String
    val horoscope_score_health: String
    val horoscope_score_energy: String

    // ── Tarot ─────────────────────────────────────────────────────────────────
    val tarot_label: String
    val tarot_title1: String
    val tarot_title2: String
    val tarot_select_period: String
    val tarot_period_day_title: String
    val tarot_period_day_desc: String
    val tarot_period_week_title: String
    val tarot_period_week_desc: String
    val tarot_period_month_title: String
    val tarot_period_month_desc: String
    val tarot_position_past: String
    val tarot_position_present: String
    val tarot_position_future: String
    /** Format: %1$s = period title */
    val tarot_cta_title: String
    val tarot_cta_desc: String
    val tarot_btn_loading: String
    val tarot_btn_open: String
    val tarot_ad_badge: String
    val tarot_ad_not_ready: String
    val tarot_btn_already_drawn: String
    val tarot_loading_hint: String
    val tarot_already_drawn_hint: String
    val tarot_summary_label: String
    val tarot_reversed: String
    val tarot_share_btn: String
    val tarot_share_tagline: String
    val horoscope_share_tagline: String
    // CTA «Твой персональный гороскоп» — под карточкой прогноза
    val horoscope_cta_personal_label: String
    val horoscope_cta_personal_title: String
    val daycard_label: String
    val daycard_soon: String
    val daycard_subtitle: String
    val daycard_empty: String
    val daycard_close: String

    // ── Compatibility ─────────────────────────────────────────────────────────
    val compat_label: String
    val compat_title1: String
    val compat_title2: String
    val compat_coming_soon: String
    val compat_coming_soon_desc: String
    val compat_sign1: String
    val compat_sign2: String
    val compat_select_sign1: String
    val compat_select_sign2: String
    val compat_strengths: String
    val compat_challenges: String
    /** Format: %s = error message */
    val compat_error: String

    // ── Profile ───────────────────────────────────────────────────────────────
    val profile_field_name_label: String
    val profile_field_name_placeholder: String
    val profile_field_gender_label: String
    val profile_field_gender_male: String
    val profile_field_gender_female: String
    val profile_field_date_label: String
    val profile_field_date_pick: String
    val profile_field_time_label: String
    val profile_field_time_pick: String
    val profile_field_place_label: String
    val profile_field_place_pick: String
    val profile_field_language_label: String
    val profile_field_haptics_label: String
    val profile_haptics_toggle: String
    val profile_dialog_cancel: String
    val profile_dialog_confirm: String
    val profile_dialog_date_title: String
    val profile_dialog_time_title: String
    val place_picker_title: String
    val place_picker_placeholder: String
    val place_picker_hint_short: String
    val place_picker_hint_empty: String
    val place_picker_loading: String
    val place_picker_confirm: String
    val profile_btn_close: String
    val profile_btn_change: String
    val profile_stat_element: String
    val profile_stat_planet: String
    val profile_stat_period: String
    val profile_insight_label: String
    val profile_insight_loading: String

    // ── Zodiac Signs ──────────────────────────────────────────────────────────
    val sign_aries: String
    val sign_taurus: String
    val sign_gemini: String
    val sign_cancer: String
    val sign_leo: String
    val sign_virgo: String
    val sign_libra: String
    val sign_scorpio: String
    val sign_sagittarius: String
    val sign_capricorn: String
    val sign_aquarius: String
    val sign_pisces: String

    // ── Elements ──────────────────────────────────────────────────────────────
    val element_fire: String
    val element_earth: String
    val element_air: String
    val element_water: String

    // ── Planets ───────────────────────────────────────────────────────────────
    val planet_mars: String
    val planet_venus: String
    val planet_mercury: String
    val planet_moon: String
    val planet_sun: String
    val planet_jupiter: String
    val planet_saturn: String
    val planet_uranus: String
    val planet_neptune: String
    val planet_pluto: String

    // ── Zodiac Dates ──────────────────────────────────────────────────────────
    val dates_aries: String
    val dates_taurus: String
    val dates_gemini: String
    val dates_cancer: String
    val dates_leo: String
    val dates_virgo: String
    val dates_libra: String
    val dates_scorpio: String
    val dates_sagittarius: String
    val dates_capricorn: String
    val dates_aquarius: String
    val dates_pisces: String

    // ── Tarot Card Names ──────────────────────────────────────────────────────
    val tarot_fool: String
    val tarot_magician: String
    val tarot_high_priestess: String
    val tarot_empress: String
    val tarot_emperor: String
    val tarot_hierophant: String
    val tarot_lovers: String
    val tarot_chariot: String
    val tarot_strength: String
    val tarot_hermit: String
    val tarot_wheel: String
    val tarot_justice: String
    val tarot_hanged: String
    val tarot_death: String
    val tarot_temperance: String
    val tarot_devil: String
    val tarot_tower: String
    val tarot_star: String
    val tarot_moon: String
    val tarot_sun: String
    val tarot_judgement: String
    val tarot_world: String

    // ── Tarot Keywords ────────────────────────────────────────────────────────
    val tarot_kw_fool: String
    val tarot_kw_magician: String
    val tarot_kw_high_priestess: String
    val tarot_kw_empress: String
    val tarot_kw_emperor: String
    val tarot_kw_hierophant: String
    val tarot_kw_lovers: String
    val tarot_kw_chariot: String
    val tarot_kw_strength: String
    val tarot_kw_hermit: String
    val tarot_kw_wheel: String
    val tarot_kw_justice: String
    val tarot_kw_hanged: String
    val tarot_kw_death: String
    val tarot_kw_temperance: String
    val tarot_kw_devil: String
    val tarot_kw_tower: String
    val tarot_kw_star: String
    val tarot_kw_moon: String
    val tarot_kw_sun: String
    val tarot_kw_judgement: String
    val tarot_kw_world: String

    // ── Horoscope Wizard ──────────────────────────────────────────────────────
    /** Format: %1$s = period accusative */
    val wizard_cta_title: String
    val wizard_cta_desc: String
    val wizard_period_acc_daily: String
    val wizard_period_acc_week: String
    val wizard_period_acc_month: String
    val wizard_divider_daily: String
    val wizard_divider_week: String
    val wizard_divider_month: String
    /** Format: %1$s = period accusative */
    val wizard_intro_title: String
    /** Format: %1$s = sign name, %2$s = period accusative */
    val wizard_intro_subtitle: String
    val wizard_btn_watch: String
    val wizard_btn_not_now: String
    /** Format: %1$d = seconds */
    val wizard_ad_title: String
    /** Format: %1$d = seconds */
    val wizard_ad_skip: String
    val wizard_ad_error: String
    /** Format: %1$s = period, %2$s = sign name */
    val wizard_done_text: String
    val wizard_done_btn: String

    // ── HoroscopePeriod AI prompts ────────────────────────────────────────────
    val period_today_prompt: String
    val period_week_prompt: String
    val period_month_prompt: String

    // ── Onboarding — Buttons ──────────────────────────────────────────────────
    val onb_btn_skip_all: String
    val onb_btn_skip_step: String
    val onb_btn_start: String
    val onb_btn_next: String
    val onb_btn_done: String

    // ── Onboarding — Welcome ──────────────────────────────────────────────────
    val onb_welcome_title: String
    val onb_welcome_subtitle: String
    val onb_welcome_desc: String

    // ── Onboarding — Name ─────────────────────────────────────────────────────
    val onb_name_title: String
    val onb_name_subtitle: String
    val onb_name_placeholder: String

    // ── Onboarding — Gender ───────────────────────────────────────────────────
    val onb_gender_title: String
    val onb_gender_subtitle: String
    val onb_gender_male: String
    val onb_gender_female: String

    // ── Onboarding — Date ─────────────────────────────────────────────────────
    val onb_date_title: String
    val onb_date_subtitle: String

    // ── Onboarding — Time ─────────────────────────────────────────────────────
    val onb_time_title: String
    val onb_time_subtitle: String

    // ── Onboarding — Place ────────────────────────────────────────────────────
    val onb_place_title: String
    val onb_place_subtitle: String
    val onb_place_placeholder: String

    // ── Onboarding — Final ────────────────────────────────────────────────────
    val onb_final_title: String
    /** Format: %s = user name */
    val onb_final_sign_with_name: String
    val onb_final_sign: String

    // ── Onboarding — Summary ──────────────────────────────────────────────────
    val onb_summary_name: String
    val onb_summary_gender: String
    val onb_summary_birth_date: String
    val onb_summary_birth_time: String
    val onb_summary_place: String

    // ── Onboarding — Months ───────────────────────────────────────────────────
    val onb_month_jan: String
    val onb_month_feb: String
    val onb_month_mar: String
    val onb_month_apr: String
    val onb_month_may: String
    val onb_month_jun: String
    val onb_month_jul: String
    val onb_month_aug: String
    val onb_month_sep: String
    val onb_month_oct: String
    val onb_month_nov: String
    val onb_month_dec: String

    // ── Push Notifications ────────────────────────────────────────────────────
    val notif_channel_name: String
    val notif_channel_desc: String
    val notif_daily_title: String
    /** Format: %s = localized sign name */
    val notif_daily_body: String
    val push_prompt_title: String
    val push_prompt_body: String
    val push_prompt_allow: String
    val push_prompt_deny: String
    val push_status_enabled: String
    val push_status_disabled: String

    // ── Dream Decoding ────────────────────────────────────────────────────────
    val dream_nav: String
    val dream_label: String
    val dream_title1: String
    val dream_title2: String
    val dream_input_hint: String
    val dream_input_placeholder: String
    val dream_btn_decode: String
    val dream_ad_badge: String
    val dream_ad_not_ready: String
    val dream_how_title: String
    val dream_how_desc: String
    val dream_result_label: String
    val dream_btn_new: String
    val dream_loading_hint: String
    val dream_chars_limit: String
}

/** Текущие строки на основе выбранного языка. */
val str: AppStrings
    get() = LanguageManager.strings

// ── (appended) push notification strings — used outside Compose context too
// Access via: AppStrings inst
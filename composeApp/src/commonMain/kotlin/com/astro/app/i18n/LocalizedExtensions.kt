package com.astro.app.i18n

import androidx.compose.runtime.Composable
import astroapp.composeapp.generated.resources.*
import com.astro.app.data.HoroscopePeriod
import com.astro.app.data.TarotCard
import com.astro.app.data.ZodiacSign
import org.jetbrains.compose.resources.stringResource

// ── ZodiacSign extensions ─────────────────────────────────────────────────────

@Composable
fun ZodiacSign.localizedName(): String = stringResource(when (this.id) {
    "aries"       -> Res.string.sign_aries
    "taurus"      -> Res.string.sign_taurus
    "gemini"      -> Res.string.sign_gemini
    "cancer"      -> Res.string.sign_cancer
    "leo"         -> Res.string.sign_leo
    "virgo"       -> Res.string.sign_virgo
    "libra"       -> Res.string.sign_libra
    "scorpio"     -> Res.string.sign_scorpio
    "sagittarius" -> Res.string.sign_sagittarius
    "capricorn"   -> Res.string.sign_capricorn
    "aquarius"    -> Res.string.sign_aquarius
    else          -> Res.string.sign_pisces
})

@Composable
fun ZodiacSign.localizedElement(): String = stringResource(when (this.id) {
    "aries", "leo", "sagittarius"  -> Res.string.element_fire
    "taurus", "virgo", "capricorn" -> Res.string.element_earth
    "gemini", "libra", "aquarius"  -> Res.string.element_air
    else                           -> Res.string.element_water
})

@Composable
fun ZodiacSign.localizedPlanet(): String = stringResource(when (this.id) {
    "aries"       -> Res.string.planet_mars
    "taurus"      -> Res.string.planet_venus
    "gemini"      -> Res.string.planet_mercury
    "cancer"      -> Res.string.planet_moon
    "leo"         -> Res.string.planet_sun
    "virgo"       -> Res.string.planet_mercury
    "libra"       -> Res.string.planet_venus
    "scorpio"     -> Res.string.planet_pluto
    "sagittarius" -> Res.string.planet_jupiter
    "capricorn"   -> Res.string.planet_saturn
    "aquarius"    -> Res.string.planet_uranus
    else          -> Res.string.planet_neptune
})

@Composable
fun ZodiacSign.localizedDates(): String = stringResource(when (this.id) {
    "aries"       -> Res.string.dates_aries
    "taurus"      -> Res.string.dates_taurus
    "gemini"      -> Res.string.dates_gemini
    "cancer"      -> Res.string.dates_cancer
    "leo"         -> Res.string.dates_leo
    "virgo"       -> Res.string.dates_virgo
    "libra"       -> Res.string.dates_libra
    "scorpio"     -> Res.string.dates_scorpio
    "sagittarius" -> Res.string.dates_sagittarius
    "capricorn"   -> Res.string.dates_capricorn
    "aquarius"    -> Res.string.dates_aquarius
    else          -> Res.string.dates_pisces
})

// ── TarotCard extensions ──────────────────────────────────────────────────────

@Composable
fun TarotCard.localizedName(): String = stringResource(when (this.resourceKey) {
    "fool"             -> Res.string.tarot_fool
    "magician"         -> Res.string.tarot_magician
    "high_priestess"   -> Res.string.tarot_high_priestess
    "empress"          -> Res.string.tarot_empress
    "emperor"          -> Res.string.tarot_emperor
    "hierophant"       -> Res.string.tarot_hierophant
    "lovers"           -> Res.string.tarot_lovers
    "chariot"          -> Res.string.tarot_chariot
    "strength"         -> Res.string.tarot_strength
    "hermit"           -> Res.string.tarot_hermit
    "wheel_of_fortune" -> Res.string.tarot_wheel
    "justice"          -> Res.string.tarot_justice
    "hanged_man"       -> Res.string.tarot_hanged
    "death"            -> Res.string.tarot_death
    "temperance"       -> Res.string.tarot_temperance
    "devil"            -> Res.string.tarot_devil
    "tower"            -> Res.string.tarot_tower
    "star"             -> Res.string.tarot_star
    "moon"             -> Res.string.tarot_moon
    "sun"              -> Res.string.tarot_sun
    "judgment"         -> Res.string.tarot_judgement
    else               -> Res.string.tarot_world
})

@Composable
fun TarotCard.localizedKeywords(): String = stringResource(when (this.resourceKey) {
    "fool"             -> Res.string.tarot_kw_fool
    "magician"         -> Res.string.tarot_kw_magician
    "high_priestess"   -> Res.string.tarot_kw_high_priestess
    "empress"          -> Res.string.tarot_kw_empress
    "emperor"          -> Res.string.tarot_kw_emperor
    "hierophant"       -> Res.string.tarot_kw_hierophant
    "lovers"           -> Res.string.tarot_kw_lovers
    "chariot"          -> Res.string.tarot_kw_chariot
    "strength"         -> Res.string.tarot_kw_strength
    "hermit"           -> Res.string.tarot_kw_hermit
    "wheel_of_fortune" -> Res.string.tarot_kw_wheel
    "justice"          -> Res.string.tarot_kw_justice
    "hanged_man"       -> Res.string.tarot_kw_hanged
    "death"            -> Res.string.tarot_kw_death
    "temperance"       -> Res.string.tarot_kw_temperance
    "devil"            -> Res.string.tarot_kw_devil
    "tower"            -> Res.string.tarot_kw_tower
    "star"             -> Res.string.tarot_kw_star
    "moon"             -> Res.string.tarot_kw_moon
    "sun"              -> Res.string.tarot_kw_sun
    "judgment"         -> Res.string.tarot_kw_judgement
    else               -> Res.string.tarot_kw_world
})

// ── HoroscopePeriod extensions ────────────────────────────────────────────────

@Composable
fun HoroscopePeriod.localizedLabel(): String = stringResource(when (this) {
    HoroscopePeriod.DAILY   -> Res.string.horoscope_tab_today
    HoroscopePeriod.WEEKLY  -> Res.string.horoscope_tab_week
    HoroscopePeriod.MONTHLY -> Res.string.horoscope_tab_month
})

@Composable
fun HoroscopePeriod.localizedPrompt(): String = stringResource(when (this) {
    HoroscopePeriod.DAILY   -> Res.string.period_today_prompt
    HoroscopePeriod.WEEKLY  -> Res.string.period_week_prompt
    HoroscopePeriod.MONTHLY -> Res.string.period_month_prompt
})

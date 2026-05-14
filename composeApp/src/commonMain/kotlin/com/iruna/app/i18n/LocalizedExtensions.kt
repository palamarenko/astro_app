package com.iruna.app.i18n

import androidx.compose.runtime.Composable
import iruna.composeapp.generated.resources.*
import com.iruna.app.data.HoroscopePeriod
import com.iruna.app.data.TarotCard
import com.iruna.app.data.ZodiacSign
import iruna.composeapp.generated.resources.Res
import iruna.composeapp.generated.resources.zodiac_aquarius
import iruna.composeapp.generated.resources.zodiac_aries
import iruna.composeapp.generated.resources.zodiac_cancer
import iruna.composeapp.generated.resources.zodiac_capricorn
import iruna.composeapp.generated.resources.zodiac_gemini
import iruna.composeapp.generated.resources.zodiac_leo
import iruna.composeapp.generated.resources.zodiac_libra
import iruna.composeapp.generated.resources.zodiac_pisces
import iruna.composeapp.generated.resources.zodiac_sagittarius
import iruna.composeapp.generated.resources.zodiac_scorpio
import iruna.composeapp.generated.resources.zodiac_taurus
import iruna.composeapp.generated.resources.zodiac_virgo
import iruna.composeapp.generated.resources.zodiac_icon_aquarius
import iruna.composeapp.generated.resources.zodiac_icon_aries
import iruna.composeapp.generated.resources.zodiac_icon_cancer
import iruna.composeapp.generated.resources.zodiac_icon_capricorn
import iruna.composeapp.generated.resources.zodiac_icon_gemini
import iruna.composeapp.generated.resources.zodiac_icon_leo
import iruna.composeapp.generated.resources.zodiac_icon_libra
import iruna.composeapp.generated.resources.zodiac_icon_pisces
import iruna.composeapp.generated.resources.zodiac_icon_sagittarius
import iruna.composeapp.generated.resources.zodiac_icon_scorpio
import iruna.composeapp.generated.resources.zodiac_icon_taurus
import iruna.composeapp.generated.resources.zodiac_icon_virgo
import org.jetbrains.compose.resources.painterResource

// ── ZodiacSign extensions ─────────────────────────────────────────────────────

@Composable
fun ZodiacSign.iconPainter() = painterResource(when (this.id) {
    "aries"       -> Res.drawable.zodiac_aries
    "taurus"      -> Res.drawable.zodiac_taurus
    "gemini"      -> Res.drawable.zodiac_gemini
    "cancer"      -> Res.drawable.zodiac_cancer
    "leo"         -> Res.drawable.zodiac_leo
    "virgo"       -> Res.drawable.zodiac_virgo
    "libra"       -> Res.drawable.zodiac_libra
    "scorpio"     -> Res.drawable.zodiac_scorpio
    "sagittarius" -> Res.drawable.zodiac_sagittarius
    "capricorn"   -> Res.drawable.zodiac_capricorn
    "aquarius"    -> Res.drawable.zodiac_aquarius
    else          -> Res.drawable.zodiac_pisces
})

@Composable
fun ZodiacSign.iconSmallPainter() = painterResource(when (this.id) {
    "aries"       -> Res.drawable.zodiac_icon_aries
    "taurus"      -> Res.drawable.zodiac_icon_taurus
    "gemini"      -> Res.drawable.zodiac_icon_gemini
    "cancer"      -> Res.drawable.zodiac_icon_cancer
    "leo"         -> Res.drawable.zodiac_icon_leo
    "virgo"       -> Res.drawable.zodiac_icon_virgo
    "libra"       -> Res.drawable.zodiac_icon_libra
    "scorpio"     -> Res.drawable.zodiac_icon_scorpio
    "sagittarius" -> Res.drawable.zodiac_icon_sagittarius
    "capricorn"   -> Res.drawable.zodiac_icon_capricorn
    "aquarius"    -> Res.drawable.zodiac_icon_aquarius
    else          -> Res.drawable.zodiac_icon_pisces
})

fun ZodiacSign.localizedName(): String = when (this.id) {
    "aries"       -> str.sign_aries
    "taurus"      -> str.sign_taurus
    "gemini"      -> str.sign_gemini
    "cancer"      -> str.sign_cancer
    "leo"         -> str.sign_leo
    "virgo"       -> str.sign_virgo
    "libra"       -> str.sign_libra
    "scorpio"     -> str.sign_scorpio
    "sagittarius" -> str.sign_sagittarius
    "capricorn"   -> str.sign_capricorn
    "aquarius"    -> str.sign_aquarius
    else          -> str.sign_pisces
}

fun ZodiacSign.localizedElement(): String = when (this.id) {
    "aries", "leo", "sagittarius"  -> str.element_fire
    "taurus", "virgo", "capricorn" -> str.element_earth
    "gemini", "libra", "aquarius"  -> str.element_air
    else                           -> str.element_water
}

fun ZodiacSign.localizedPlanet(): String = when (this.id) {
    "aries"       -> str.planet_mars
    "taurus"      -> str.planet_venus
    "gemini"      -> str.planet_mercury
    "cancer"      -> str.planet_moon
    "leo"         -> str.planet_sun
    "virgo"       -> str.planet_mercury
    "libra"       -> str.planet_venus
    "scorpio"     -> str.planet_pluto
    "sagittarius" -> str.planet_jupiter
    "capricorn"   -> str.planet_saturn
    "aquarius"    -> str.planet_uranus
    else          -> str.planet_neptune
}

fun ZodiacSign.localizedDates(): String = when (this.id) {
    "aries"       -> str.dates_aries
    "taurus"      -> str.dates_taurus
    "gemini"      -> str.dates_gemini
    "cancer"      -> str.dates_cancer
    "leo"         -> str.dates_leo
    "virgo"       -> str.dates_virgo
    "libra"       -> str.dates_libra
    "scorpio"     -> str.dates_scorpio
    "sagittarius" -> str.dates_sagittarius
    "capricorn"   -> str.dates_capricorn
    "aquarius"    -> str.dates_aquarius
    else          -> str.dates_pisces
}

// ── TarotCard extensions ──────────────────────────────────────────────────────

fun TarotCard.localizedName(): String = when (this.resourceKey) {
    "fool"             -> str.tarot_fool
    "magician"         -> str.tarot_magician
    "high_priestess"   -> str.tarot_high_priestess
    "empress"          -> str.tarot_empress
    "emperor"          -> str.tarot_emperor
    "hierophant"       -> str.tarot_hierophant
    "lovers"           -> str.tarot_lovers
    "chariot"          -> str.tarot_chariot
    "strength"         -> str.tarot_strength
    "hermit"           -> str.tarot_hermit
    "wheel_of_fortune" -> str.tarot_wheel
    "justice"          -> str.tarot_justice
    "hanged_man"       -> str.tarot_hanged
    "death"            -> str.tarot_death
    "temperance"       -> str.tarot_temperance
    "devil"            -> str.tarot_devil
    "tower"            -> str.tarot_tower
    "star"             -> str.tarot_star
    "moon"             -> str.tarot_moon
    "sun"              -> str.tarot_sun
    "judgment"         -> str.tarot_judgement
    else               -> str.tarot_world
}

fun TarotCard.localizedKeywords(): String = when (this.resourceKey) {
    "fool"             -> str.tarot_kw_fool
    "magician"         -> str.tarot_kw_magician
    "high_priestess"   -> str.tarot_kw_high_priestess
    "empress"          -> str.tarot_kw_empress
    "emperor"          -> str.tarot_kw_emperor
    "hierophant"       -> str.tarot_kw_hierophant
    "lovers"           -> str.tarot_kw_lovers
    "chariot"          -> str.tarot_kw_chariot
    "strength"         -> str.tarot_kw_strength
    "hermit"           -> str.tarot_kw_hermit
    "wheel_of_fortune" -> str.tarot_kw_wheel
    "justice"          -> str.tarot_kw_justice
    "hanged_man"       -> str.tarot_kw_hanged
    "death"            -> str.tarot_kw_death
    "temperance"       -> str.tarot_kw_temperance
    "devil"            -> str.tarot_kw_devil
    "tower"            -> str.tarot_kw_tower
    "star"             -> str.tarot_kw_star
    "moon"             -> str.tarot_kw_moon
    "sun"              -> str.tarot_kw_sun
    "judgment"         -> str.tarot_kw_judgement
    else               -> str.tarot_kw_world
}

// ── HoroscopePeriod extensions ────────────────────────────────────────────────

fun HoroscopePeriod.localizedLabel(): String = when (this) {
    HoroscopePeriod.DAILY   -> str.horoscope_tab_today
    HoroscopePeriod.WEEKLY  -> str.horoscope_tab_week
    HoroscopePeriod.MONTHLY -> str.horoscope_tab_month
}

fun HoroscopePeriod.localizedPrompt(): String = when (this) {
    HoroscopePeriod.DAILY   -> str.period_today_prompt
    HoroscopePeriod.WEEKLY  -> str.period_week_prompt
    HoroscopePeriod.MONTHLY -> str.period_month_prompt
}

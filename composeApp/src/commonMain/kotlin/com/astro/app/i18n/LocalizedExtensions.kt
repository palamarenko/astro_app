package com.astro.app.i18n

import com.astro.app.data.ZodiacSign
import com.astro.app.data.TarotCard
import com.astro.app.data.HoroscopePeriod

// ── ZodiacSign extensions ─────────────────────────────────────────────────────
fun ZodiacSign.localizedName(s: StringBundle): String = when (this.emoji) {
    "♈" -> s.signAries
    "♉" -> s.signTaurus
    "♊" -> s.signGemini
    "♋" -> s.signCancer
    "♌" -> s.signLeo
    "♍" -> s.signVirgo
    "♎" -> s.signLibra
    "♏" -> s.signScorpio
    "♐" -> s.signSagittarius
    "♑" -> s.signCapricorn
    "♒" -> s.signAquarius
    "♓" -> s.signPisces
    else -> this.name
}

fun ZodiacSign.localizedElement(s: StringBundle): String = when (this.element) {
    "Огонь", "Вогонь", "Fire"   -> s.elementFire
    "Земля", "Earth"            -> s.elementEarth
    "Воздух", "Повітря", "Air"  -> s.elementAir
    "Вода", "Water"             -> s.elementWater
    else -> this.element
}

fun ZodiacSign.localizedPlanet(s: StringBundle): String = when (this.planet) {
    "Марс", "Mars"              -> s.planetMars
    "Венера", "Venus"           -> s.planetVenus
    "Меркурий", "Меркурій", "Mercury" -> s.planetMercury
    "Луна", "Місяць", "Moon"    -> s.planetMoon
    "Солнце", "Сонце", "Sun"    -> s.planetSun
    "Юпитер", "Юпітер", "Jupiter" -> s.planetJupiter
    "Сатурн", "Saturn"          -> s.planetSaturn
    "Уран", "Uranus"            -> s.planetUranus
    "Нептун", "Neptune"         -> s.planetNeptune
    "Плутон", "Pluto"           -> s.planetPluto
    else -> this.planet
}

fun ZodiacSign.localizedDates(s: StringBundle): String = when (this.emoji) {
    "♈" -> s.datesAries
    "♉" -> s.datesTaurus
    "♊" -> s.datesGemini
    "♋" -> s.datesCancer
    "♌" -> s.datesLeo
    "♍" -> s.datesVirgo
    "♎" -> s.datesLibra
    "♏" -> s.datesScorpio
    "♐" -> s.datesSagittarius
    "♑" -> s.datesCapricorn
    "♒" -> s.datesAquarius
    "♓" -> s.datesPisces
    else -> this.dates
}

// ── TarotCard extensions ──────────────────────────────────────────────────────
fun TarotCard.localizedName(s: StringBundle): String = when (this.number) {
    "0"     -> s.tarotFool
    "I"     -> s.tarotMagician
    "II"    -> s.tarotHighPriestess
    "III"   -> s.tarotEmpress
    "IV"    -> s.tarotEmperor
    "V"     -> s.tarotHierophant
    "VI"    -> s.tarotLovers
    "VII"   -> s.tarotChariot
    "VIII"  -> s.tarotStrength
    "IX"    -> s.tarotHermit
    "X"     -> s.tarotWheel
    "XI"    -> s.tarotJustice
    "XII"   -> s.tarotHanged
    "XIII"  -> s.tarotDeath
    "XIV"   -> s.tarotTemperance
    "XV"    -> s.tarotDevil
    "XVI"   -> s.tarotTower
    "XVII"  -> s.tarotStar
    "XVIII" -> s.tarotMoon
    "XIX"   -> s.tarotSun
    "XX"    -> s.tarotJudgement
    "XXI"   -> s.tarotWorld
    else -> this.name
}

fun TarotCard.localizedKeywords(s: StringBundle): String = when (this.number) {
    "0"     -> s.tarotKwFool
    "I"     -> s.tarotKwMagician
    "II"    -> s.tarotKwHighPriestess
    "III"   -> s.tarotKwEmpress
    "IV"    -> s.tarotKwEmperor
    "V"     -> s.tarotKwHierophant
    "VI"    -> s.tarotKwLovers
    "VII"   -> s.tarotKwChariot
    "VIII"  -> s.tarotKwStrength
    "IX"    -> s.tarotKwHermit
    "X"     -> s.tarotKwWheel
    "XI"    -> s.tarotKwJustice
    "XII"   -> s.tarotKwHanged
    "XIII"  -> s.tarotKwDeath
    "XIV"   -> s.tarotKwTemperance
    "XV"    -> s.tarotKwDevil
    "XVI"   -> s.tarotKwTower
    "XVII"  -> s.tarotKwStar
    "XVIII" -> s.tarotKwMoon
    "XIX"   -> s.tarotKwSun
    "XX"    -> s.tarotKwJudgement
    "XXI"   -> s.tarotKwWorld
    else -> this.keywords
}

// ── HoroscopePeriod extensions ────────────────────────────────────────────────
fun HoroscopePeriod.localizedLabel(s: StringBundle): String = when (this) {
    HoroscopePeriod.TODAY -> s.horoscopeTabToday
    HoroscopePeriod.WEEK  -> s.horoscopeTabWeek
    HoroscopePeriod.MONTH -> s.horoscopeTabMonth
}

fun HoroscopePeriod.localizedPrompt(s: StringBundle): String = when (this) {
    HoroscopePeriod.TODAY -> s.periodTodayPrompt
    HoroscopePeriod.WEEK  -> s.periodWeekPrompt
    HoroscopePeriod.MONTH -> s.periodMonthPrompt
}

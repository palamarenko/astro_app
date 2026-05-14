package com.iruna.app.data

import kotlinx.serialization.Serializable

// ── Zodiac ───────────────────────────────────────────────────────────────────
data class ZodiacSign(
    val id: String,     // English slug (e.g. "aries")
    val name: String,   // RU display name
    val emoji: String,
    val dates: String,  // RU display dates
    val element: String,
    val planet: String,
)

val ALL_SIGNS = listOf(
    ZodiacSign("aries",       "Овен",     "♈", "21 мар – 19 апр", "Огонь",  "Марс"),
    ZodiacSign("taurus",      "Телец",    "♉", "20 апр – 20 май", "Земля",  "Венера"),
    ZodiacSign("gemini",      "Близнецы", "♊", "21 май – 20 июн", "Воздух", "Меркурий"),
    ZodiacSign("cancer",      "Рак",      "♋", "21 июн – 22 июл", "Вода",   "Луна"),
    ZodiacSign("leo",         "Лев",      "♌", "23 июл – 22 авг", "Огонь",  "Солнце"),
    ZodiacSign("virgo",       "Дева",     "♍", "23 авг – 22 сен", "Земля",  "Меркурий"),
    ZodiacSign("libra",       "Весы",     "♎", "23 сен – 22 окт", "Воздух", "Венера"),
    ZodiacSign("scorpio",     "Скорпион", "♏", "23 окт – 21 ноя", "Вода",   "Плутон"),
    ZodiacSign("sagittarius", "Стрелец",  "♐", "22 ноя – 21 дек", "Огонь",  "Юпитер"),
    ZodiacSign("capricorn",   "Козерог",  "♑", "22 дек – 19 янв", "Земля",  "Сатурн"),
    ZodiacSign("aquarius",    "Водолей",  "♒", "20 янв – 18 фев", "Воздух", "Уран"),
    ZodiacSign("pisces",      "Рыбы",     "♓", "19 фев – 20 мар", "Вода",   "Нептун"),
)

// ── Tarot ────────────────────────────────────────────────────────────────────
data class TarotCard(
    val name: String,         // RU fallback
    val number: String,       // Roman numeral key — language-independent
    val symbol: String,
    val keywords: String,     // RU fallback
    val reversed: Boolean = false,
    val resourceKey: String = "", // maps to composeResources drawable name
)

val ALL_TAROT = listOf(
    TarotCard("Шут",              "0",     "🃏", "Начало, свобода, риск",            resourceKey = "fool"),
    TarotCard("Маг",              "I",     "✦",  "Воля, мастерство, действие",       resourceKey = "magician"),
    TarotCard("Верховная жрица",  "II",    "☽",  "Интуиция, тайна, мудрость",        resourceKey = "high_priestess"),
    TarotCard("Императрица",      "III",   "♀",  "Плодородие, красота, забота",      resourceKey = "empress"),
    TarotCard("Император",        "IV",    "♂",  "Власть, структура, защита",        resourceKey = "emperor"),
    TarotCard("Иерофант",         "V",     "⛩",  "Традиция, духовность, обряд",      resourceKey = "hierophant"),
    TarotCard("Влюблённые",       "VI",    "♡",  "Союз, выбор, гармония",            resourceKey = "lovers"),
    TarotCard("Колесница",        "VII",   "☀",  "Победа, контроль, движение",       resourceKey = "chariot"),
    TarotCard("Сила",             "VIII",  "∞",  "Мужество, терпение, страсть",      resourceKey = "strength"),
    TarotCard("Отшельник",        "IX",    "◎",  "Одиночество, поиск, мудрость",     resourceKey = "hermit"),
    TarotCard("Колесо Фортуны",   "X",     "⊕",  "Судьба, цикл, удача",              resourceKey = "wheel_of_fortune"),
    TarotCard("Справедливость",   "XI",    "⚖",  "Баланс, истина, закон",            resourceKey = "justice"),
    TarotCard("Повешенный",       "XII",   "⧖",  "Жертва, пауза, откровение",        resourceKey = "hanged_man"),
    TarotCard("Смерть",           "XIII",  "☾",  "Трансформация, конец, обновление", resourceKey = "death"),
    TarotCard("Умеренность",      "XIV",   "≋",  "Равновесие, терпение, цель",       resourceKey = "temperance"),
    TarotCard("Дьявол",           "XV",    "⛧",  "Искушение, цепи, страсть",         resourceKey = "devil"),
    TarotCard("Башня",            "XVI",   "⚡",  "Потрясение, перемены, кризис",     resourceKey = "tower"),
    TarotCard("Звезда",           "XVII",  "★",  "Надежда, вдохновение, мечта",      resourceKey = "star"),
    TarotCard("Луна",             "XVIII", "🌙", "Иллюзия, страх, подсознание",      resourceKey = "moon"),
    TarotCard("Солнце",           "XIX",   "☀",  "Радость, успех, жизненность",      resourceKey = "sun"),
    TarotCard("Суд",              "XX",    "♩",  "Пробуждение, прощение, призыв",    resourceKey = "judgment"),
    TarotCard("Мир",              "XXI",   "◯",  "Завершение, единство, триумф",     resourceKey = "world"),
)

// ── API Response models ───────────────────────────────────────────────────────
@Serializable
data class TarotCardContent(
    val past: String = "",
    val present: String = "",
    val future: String = "",
)

@Serializable
data class HoroscopeResponse(
    val text: String,
    val keyword: String = "",
    val love: Int, val career: Int, val health: Int, val energy: Int,
)

@Serializable
data class CompatibilityResponse(
    val score: Int, val title: String, val text: String,
    val strengths: String, val challenges: String,
)

@Serializable
data class TarotReadingResponse(
    val past: String, val present: String, val future: String, val summary: String,
)

// ── Enums ─────────────────────────────────────────────────────────────────────
enum class HoroscopePeriod(val id: String, val label: String, val promptRu: String) {
    DAILY("daily", "Сегодня", "на сегодня"),
    WEEKLY("weekly", "Неделя",   "на эту неделю"),
    MONTHLY("monthly", "Месяц",   "на этот месяц"),
}

enum class BottomTab { HOROSCOPE, TAROT, COMPATIBILITY, PROFILE }

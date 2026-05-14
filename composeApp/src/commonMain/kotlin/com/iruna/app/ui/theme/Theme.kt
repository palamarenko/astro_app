package com.iruna.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colors ───────────────────────────────────────────────────────────────────
object AppColors {
    val Background    = Color(0xFF090910)
    val Card          = Color(0xFF111119)
    val CardDark      = Color(0xFF0D0D15)
    val Surface       = Color(0xFF171724)
    val AccentGold    = Color(0xFFBE9A4A)
    val AccentGoldDim = Color(0xFF7A6030)
    val TextPrimary   = Color(0xFFF2EEE6)
    val TextSecondary = Color(0xFFB8B0A0)
    val TextMuted     = Color(0xFF666666)
    val TextDim       = Color(0xFF3A3A3A)
    val Border        = Color(0xFF222230)
    val BorderDark    = Color(0xFF18182A)
    val NavBackground = Color(0xF2090910)

    // Elements — все в тёплых золотисто-нейтральных тонах, без ярких цветов
    val Fire  = Color(0xFFB8915A)   // тёплый янтарь
    val Earth = Color(0xFF8A7D62)   // тёплый камень
    val Air   = Color(0xFF7A8FA0)   // приглушённый стальной
    val Water = Color(0xFF6A7EA8)   // тёмный стальной синий

    fun elementColor(element: String): Color = when (element) {
        "Огонь"  -> Fire
        "Земля"  -> Earth
        "Воздух" -> Air
        "Вода"   -> Water
        else     -> AccentGold
    }
}

// ── Spacing ──────────────────────────────────────────────────────────────────
object Spacing {
    val xs  = 4.dp
    val s   = 8.dp
    val m   = 12.dp
    val l   = 16.dp
    val xl  = 20.dp
    val xxl = 24.dp
}

// ── Radius ───────────────────────────────────────────────────────────────────
object Radius {
    val s    = 9.dp
    val m    = 13.dp
    val l    = 16.dp
    val xl   = 20.dp
    val full = 100.dp
}

// ── Type scale ───────────────────────────────────────────────────────────────
object AppType {
    val label   = 10.sp
    val caption = 11.sp
    val body    = 14.sp
    val bodyLg  = 15.sp
    val title   = 20.sp
    val h2      = 26.sp
    val h1      = 30.sp
}

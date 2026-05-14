package com.iruna.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colors ───────────────────────────────────────────────────────────────────
object AppColors {
    val Background    = Color(0xFF0A0A0F)
    val Card          = Color(0xFF13131F)
    val CardDark      = Color(0xFF0F0F1A)
    val Surface       = Color(0xFF1C1C2E)
    val AccentGold    = Color(0xFFC9A84C)
    val TextPrimary   = Color(0xFFF0ECE4)
    val TextSecondary = Color(0xFFD0C8B8)
    val TextMuted     = Color(0xFF888888)
    val TextDim       = Color(0xFF555555)
    val Border        = Color(0xFF2A2A3A)
    val BorderDark    = Color(0xFF1E1E2E)
    val NavBackground = Color(0xF20A0A0F) // 95% opacity

    // Elements
    val Fire  = Color(0xFFE07A5F)
    val Earth = Color(0xFF8AAB7A)
    val Air   = Color(0xFF7EC8E3)
    val Water = Color(0xFF7B8DE8)

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

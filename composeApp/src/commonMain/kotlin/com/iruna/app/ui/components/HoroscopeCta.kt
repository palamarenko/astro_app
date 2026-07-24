package com.iruna.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.i18n.str

// ── CTA «Твой персональный гороскоп» (золотая фольга, вариант 1) ───────────────
// Три анимации: пульсирующее золотое свечение, бесшовное мерцание градиента
// по тексту и мягкий диагональный световой блик по всей кнопке.
// Без blur() — он только на Android; здесь KMP, поэтому свечение — радиальный
// градиент. Горизонтальные отступы задаёт вызывающая сторона.
@Composable
fun HoroscopeCta(onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "cta")
    // 1. Пульсирующее золотое свечение (glow) позади кнопки
    val glow by inf.animateFloat(
        0.35f, 0.65f,
        infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "ctaGlow",
    )
    // 2. Мерцание текста — бесшовный сдвиг градиента ровно на один период (0→1).
    // Вместе с TileMode.Repeated и совпадающими крайними цветами даёт петлю без рывка.
    val shine by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3400, easing = LinearEasing)),
        "ctaShine",
    )
    // 3. Световой блик — плавно проходит слева направо; за краями кнопки полоса
    // уходит из зоны видимости, что даёт естественную паузу между проходами.
    val sweep by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing)),
        "ctaSweep",
    )

    val shape = RoundedCornerShape(18.dp)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat()

        // ── Свечение позади кнопки (радиальный ореол) ─────────────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = 1.04f
                    scaleY = 1.14f
                    alpha = glow
                }
                .clip(shape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFE6B45A).copy(alpha = 0.45f), Color.Transparent)
                    )
                ),
        )

        // ── Кнопка ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF1C1109),
                            0.45f to Color(0xFF2C1A0D),
                            1.00f to Color(0xFF1C1109),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    )
                )
                .border(1.dp, Color(0xFFE6BE78).copy(alpha = 0.55f), shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // Контент
            Column(
                modifier = Modifier.padding(vertical = 18.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = str.horoscope_cta_personal_label,
                    color = Color(0xFFB89860),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = TextUnit(3f, TextUnitType.Sp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = str.horoscope_cta_personal_title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = TextUnit(0.5f, TextUnitType.Sp),
                    style = LocalTextStyle.current.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8A6A2E),
                                Color(0xFFF5DFA0),
                                Color(0xFFFFF8E0),
                                Color(0xFFF5DFA0),
                                Color(0xFF8A6A2E),
                            ),
                            start = Offset(shine * widthPx, 0f),
                            end = Offset(shine * widthPx + widthPx, 0f),
                            tileMode = TileMode.Repeated,
                        )
                    ),
                )
            }

            // Световой блик — мягкая диагональная полоса поверх всей кнопки.
            // Рисуем через drawBehind (фаза draw): полоса задаётся градиентом
            // transparent→свет→transparent с TileMode.Clamp, поэтому вне полосы
            // всё прозрачно, а из-за отсутствия поворота нет обрезки по углам.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val ux = 0.94f       // ось блика: вправо + чуть вниз → лёгкий наклон
                        val uy = 0.34f
                        val half = w * 0.16f // половина толщины полосы
                        val cx = -0.25f * w + sweep * (1.5f * w)
                        val cy = h / 2f
                        drawRect(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.5f to Color(0xFFFFF4DA).copy(alpha = 0.16f),
                                    1f to Color.Transparent,
                                ),
                                start = Offset(cx - ux * half, cy - uy * half),
                                end = Offset(cx + ux * half, cy + uy * half),
                                tileMode = TileMode.Clamp,
                            ),
                        )
                    },
            )
        }
    }
}

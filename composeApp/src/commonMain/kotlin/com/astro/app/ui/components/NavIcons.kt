package com.astro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Все иконки нарисованы в системе координат 22x22, как в исходных SVG.
// Размер контролируется параметром [size]; цвет — параметром [color].

@Composable
fun HoroscopeNavIcon(color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 22f
        val sw = 1.5f * s
        val cx = 11f * s
        val cy = 11f * s
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)

        // Центральная окружность (солнце)
        drawCircle(color = color, radius = 4.5f * s, center = Offset(cx, cy), style = stroke)

        // Лучи
        val rays = listOf(
            Offset(11f * s, 1f * s)    to Offset(11f * s, 4f * s),
            Offset(11f * s, 18f * s)   to Offset(11f * s, 21f * s),
            Offset(1f * s,  11f * s)   to Offset(4f * s,  11f * s),
            Offset(18f * s, 11f * s)   to Offset(21f * s, 11f * s),
            Offset(3.5f * s, 3.5f * s) to Offset(5.7f * s, 5.7f * s),
            Offset(16.3f * s, 16.3f * s) to Offset(18.5f * s, 18.5f * s),
            Offset(18.5f * s, 3.5f * s) to Offset(16.3f * s, 5.7f * s),
            Offset(5.7f * s, 16.3f * s) to Offset(3.5f * s, 18.5f * s),
        )
        rays.forEach { (a, b) ->
            drawLine(color = color, start = a, end = b, strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun TarotNavIcon(color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 22f
        val sw = 1.5f * s
        val sw2 = 1.2f * s

        // Карта (rect 12x18 со скруглением 2)
        val left = 5f * s
        val top  = 2f * s
        val w = 12f * s
        val h = 18f * s
        val r = 2f * s

        // Заливка
        drawRoundRect(
            color = color.copy(alpha = 0.08f),
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        // Обводка
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width = sw),
        )

        // Текстовые линии
        drawLine(
            color = color,
            start = Offset(8f * s, 7f * s), end = Offset(14f * s, 7f * s),
            strokeWidth = sw2, cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(8f * s, 10f * s), end = Offset(14f * s, 10f * s),
            strokeWidth = sw2, cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(8f * s, 13f * s), end = Offset(11f * s, 13f * s),
            strokeWidth = sw2, cap = StrokeCap.Round,
        )
    }
}

@Composable
fun CompatibilityNavIcon(color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 22f
        val sw = 1.5f * s
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)

        drawCircle(color = color, radius = 4f * s, center = Offset(7.5f * s, 11f * s), style = stroke)
        drawCircle(color = color, radius = 4f * s, center = Offset(14.5f * s, 11f * s), style = stroke)
    }
}

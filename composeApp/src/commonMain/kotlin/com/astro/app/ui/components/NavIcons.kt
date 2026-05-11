package com.astro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f

        // 4-pointed sparkle star (ic_zodiac shape)
        // Vertical petal: elongated top-bottom points
        val starPath = Path().apply {
            // Top point
            moveTo(cx, cy - 10f * s)
            // Right side of top petal → right point
            cubicTo(
                cx + 1.2f * s, cy - 3.5f * s,
                cx + 3.5f * s, cy - 1.2f * s,
                cx + 10f * s, cy
            )
            // Right side of right petal → bottom point
            cubicTo(
                cx + 3.5f * s, cy + 1.2f * s,
                cx + 1.2f * s, cy + 3.5f * s,
                cx, cy + 10f * s
            )
            // Left side of bottom petal → left point
            cubicTo(
                cx - 1.2f * s, cy + 3.5f * s,
                cx - 3.5f * s, cy + 1.2f * s,
                cx - 10f * s, cy
            )
            // Left side of left petal → top point
            cubicTo(
                cx - 3.5f * s, cy - 1.2f * s,
                cx - 1.2f * s, cy - 3.5f * s,
                cx, cy - 10f * s
            )
            close()
        }
        drawPath(path = starPath, color = color)

        // 4 small dots at diagonal corners
        val dotR = 0.85f * s
        val dotOffset = 6.2f * s
        drawCircle(color = color, radius = dotR, center = Offset(cx - dotOffset, cy - dotOffset))
        drawCircle(color = color, radius = dotR, center = Offset(cx + dotOffset, cy - dotOffset))
        drawCircle(color = color, radius = dotR, center = Offset(cx - dotOffset, cy + dotOffset))
        drawCircle(color = color, radius = dotR, center = Offset(cx + dotOffset, cy + dotOffset))
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

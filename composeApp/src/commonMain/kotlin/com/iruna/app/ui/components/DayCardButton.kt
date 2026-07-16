package com.iruna.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Круглая анимированная кнопка «Карта дня» для экрана гороскопов.
 * Вращающееся золото-фиолетовое кольцо, пульсирующее свечение, искры,
 * тёмный диск с парящей иконкой таро-карты.
 */
@Composable
fun DayCardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val gold      = Color(0xFFE8C874)
    val goldCream = Color(0xFFF3E6C8)
    val purple    = Color(0xFF6B4FA0)

    val inf = rememberInfiniteTransition(label = "dayCard")
    val ringAngle by inf.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )
    val glow by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val floatT by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    val sparkT by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "spark",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(180), label = "press")

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // ── Свечение ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 1f + 0.06f * glow
                    scaleX = s; scaleY = s
                    alpha = 0.55f + 0.35f * glow
                }
                .background(
                    Brush.radialGradient(
                        listOf(gold.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape,
                )
        )

        // ── Кольцо + искры ────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val ringStroke = this.size.minDimension * 0.055f
            val ringRadius = this.size.minDimension / 2f - ringStroke / 2f - 1f

            rotate(ringAngle, pivot = c) {
                drawCircle(
                    brush  = Brush.sweepGradient(
                        listOf(gold, purple, gold, purple, gold),
                        center = c,
                    ),
                    radius = ringRadius,
                    center = c,
                    style  = Stroke(width = ringStroke),
                )
            }

            // Искры — вылетают наружу и гаснут
            val n = 10
            for (i in 0 until n) {
                val phase = ((sparkT + i.toFloat() / n) % 1f)
                val a = sin(phase * PI).toFloat()               // мягкое появление/затухание
                if (a <= 0.02f) continue
                val angle = (i.toFloat() / n) * 2f * PI.toFloat() + if (i % 2 == 0) 0f else 0.3f
                val r = ringRadius * (0.5f + 0.55f * phase)
                val pos = Offset(c.x + cos(angle) * r, c.y + sin(angle) * r)
                val sparkR = (ringRadius * 0.05f) * (0.6f + 0.4f * (i % 3))
                val col = if (i % 2 == 0) gold else goldCream
                drawCircle(col.copy(alpha = 0.25f * a), radius = sparkR * 2.1f, center = pos)
                drawCircle(col.copy(alpha = a), radius = sparkR, center = pos)
            }
        }

        // ── Внутренний диск ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF2C2049), Color(0xFF1A1230), Color(0xFF120C22))
                    )
                )
                .border(1.dp, gold.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // ── Иконка таро-карты (плавает) ───────────────────────────────────
            Canvas(
                modifier = Modifier
                    .size(size * 0.30f, size * 0.48f)
                    .graphicsLayer {
                        translationY = (-3f * floatT).dp.toPx()
                        rotationZ = -2f + 4f * floatT
                    }
            ) {
                val sx = this.size.width / 60f
                val sy = this.size.height / 96f
                fun pt(x: Float, y: Float) = Offset(x * sx, y * sy)
                val sw = sx // масштаб толщины линий

                // Внешняя рамка карты
                drawRoundRect(
                    color = Color(0xFF1C1330),
                    topLeft = pt(1.5f, 1.5f),
                    size = Size(57f * sx, 93f * sy),
                    cornerRadius = CornerRadius(6f * sx, 6f * sy),
                )
                drawRoundRect(
                    color = gold,
                    topLeft = pt(1.5f, 1.5f),
                    size = Size(57f * sx, 93f * sy),
                    cornerRadius = CornerRadius(6f * sx, 6f * sy),
                    style = Stroke(width = 1.6f * sw),
                )
                // Внутренняя рамка
                drawRoundRect(
                    color = gold.copy(alpha = 0.6f),
                    topLeft = pt(6f, 6f),
                    size = Size(48f * sx, 84f * sy),
                    cornerRadius = CornerRadius(3.5f * sx, 3.5f * sy),
                    style = Stroke(width = 0.8f * sw),
                )
                // Круг с крестом
                drawCircle(gold, radius = 11f * sx, center = pt(30f, 30f), style = Stroke(width = 1.1f * sw))
                drawLine(gold, pt(30f, 21f), pt(30f, 39f), strokeWidth = 1f * sw, cap = StrokeCap.Round)
                drawLine(gold, pt(21f, 30f), pt(39f, 30f), strokeWidth = 1f * sw, cap = StrokeCap.Round)
                // Ромб
                val diamond = Path().apply {
                    moveTo(pt(30f, 46f).x, pt(30f, 46f).y)
                    lineTo(pt(38f, 60f).x, pt(38f, 60f).y)
                    lineTo(pt(30f, 74f).x, pt(30f, 74f).y)
                    lineTo(pt(22f, 60f).x, pt(22f, 60f).y)
                    close()
                }
                drawPath(diamond, gold, style = Stroke(width = 1.1f * sw, join = StrokeJoin.Round))
                // Центральная точка
                drawCircle(gold, radius = 2.4f * sx, center = pt(30f, 60f))
            }
        }
    }
}

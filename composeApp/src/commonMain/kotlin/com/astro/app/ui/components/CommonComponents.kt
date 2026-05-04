package com.astro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astro.app.ui.theme.AppColors
import com.astro.app.ui.theme.AppType
import com.astro.app.ui.theme.Radius
import com.astro.app.ui.theme.Spacing

// ── GoldButton ────────────────────────────────────────────────────────────────
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        AppColors.AccentGold.copy(alpha = if (enabled) 0.15f else 0.06f),
                        AppColors.AccentGold.copy(alpha = if (enabled) 0.08f else 0.03f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        AppColors.AccentGold.copy(alpha = if (enabled) glowAlpha else 0.2f),
                        AppColors.AccentGold.copy(alpha = if (enabled) glowAlpha * 0.6f else 0.15f),
                    )
                ),
                shape = RoundedCornerShape(Radius.xl)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = AppType.body,
            fontWeight = FontWeight.Normal,
            color = if (enabled) AppColors.AccentGold else AppColors.TextDim,
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em),
        )
    }
}

// ── SectionLabel ─────────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = AppType.label,
        color = AppColors.AccentGold.copy(alpha = 0.7f),
        fontWeight = FontWeight.Normal,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.22f, androidx.compose.ui.unit.TextUnitType.Em),
        modifier = modifier
    )
}

// ── ElementPill ───────────────────────────────────────────────────────────────
@Composable
fun ElementPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.full))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = AppType.caption,
            color = color,
            fontWeight = FontWeight.Normal,
        )
    }
}

// ── ScoreCard ─────────────────────────────────────────────────────────────────
@Composable
fun ScoreCard(
    label: String,
    icon: String,
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "score"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    fontSize = AppType.label,
                    color = AppColors.TextDim,
                )
                Text(
                    text = icon,
                    fontSize = AppType.label,
                    color = color,
                )
            }
            Spacer(Modifier.height(6.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AppColors.Surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedScore / 100f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(color.copy(alpha = 0.88f), color)
                            )
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = score.toString(),
                fontSize = AppType.h2,
                fontWeight = FontWeight.Light,
                color = AppColors.TextSecondary,
            )
        }
    }
}

// ── LoadingDots ───────────────────────────────────────────────────────────────
@Composable
fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(AppColors.AccentGold)
            )
        }
    }
}

// ── StarBackground ─────────────────────────────────────────────────────────────
// Drawn via Canvas — simple random dots with twinkle animation
@Composable
fun StarryBackground(modifier: Modifier = Modifier) {
    // Use a Box with dark background; real stars would be Canvas in production
    Box(
        modifier = modifier.background(AppColors.Background)
    )
}

package com.astro.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import astroapp.composeapp.generated.resources.*
import com.astro.app.ui.components.SectionLabel
import com.astro.app.ui.theme.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ComingSoonScreen(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "cs")

    val glowAlpha by inf.animateFloat(
        initialValue = 0.06f,
        targetValue  = 0.18f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "glow"
    )
    val starAlpha by inf.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "star"
    )

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Фоновое свечение
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentGold.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Звёзды
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(0.5f, 1f, 0.5f).forEach { scale ->
                    Text(
                        text = "✦",
                        fontSize = TextUnit(14f * scale, TextUnitType.Sp),
                        color = AppColors.AccentGold.copy(alpha = starAlpha * scale),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            SectionLabel(stringResource(Res.string.compat_label))

            Spacer(Modifier.height(Spacing.m))

            Text(
                text = stringResource(Res.string.compat_title1),
                fontSize = AppType.h1,
                fontWeight = FontWeight.Light,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.compat_title2),
                fontSize = AppType.h1,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xxl))

            Text(
                text = stringResource(Res.string.compat_coming_soon),
                fontSize = TextUnit(28f, TextUnitType.Sp),
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                color = AppColors.AccentGold.copy(alpha = 0.85f),
                letterSpacing = TextUnit(0.12f, TextUnitType.Em),
            )

            Spacer(Modifier.height(Spacing.m))

            Text(
                text = stringResource(Res.string.compat_coming_soon_desc),
                fontSize = TextUnit(13f, TextUnitType.Sp),
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = TextUnit(20f, TextUnitType.Sp),
                modifier = Modifier
                    .padding(horizontal = Spacing.xxl)
                    .alpha(0.8f)
            )
        }
    }
}

package com.iruna.app.ui.screens.dream

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.ads.rememberAdManager
import com.iruna.app.i18n.str
import com.iruna.app.ui.components.SectionLabel
import com.iruna.app.ui.theme.*
import iruna.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun DreamScreen(
    vm: DreamViewModel,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()
    val adManager = rememberAdManager()
    val adNotReadyMsg = str.dream_ad_not_ready

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // ── Decorative star background ────────────────────────────────────────
        Image(
            painter = painterResource(Res.drawable.ic_star),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-16).dp, y = 48.dp)
                .graphicsLayer { alpha = 0.40f },
        )
        Image(
            painter = painterResource(Res.drawable.ic_star),
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopStart)
                .offset(x = 20.dp, y = 120.dp)
                .graphicsLayer { alpha = 0.15f },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header: title left, image right ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxl)
            ) {
                // Hero image — top right, covers ~half width
                Image(
                    painter = painterResource(Res.drawable.dream_header),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(180.dp)
                        .align(Alignment.TopEnd)
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp, topEnd = 0.dp,
                                bottomStart = 20.dp, bottomEnd = 0.dp
                            )
                        )
                        .graphicsLayer { alpha = 0.88f },
                )
                // Gradient overlay to blend image into background
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(180.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.Background, Color.Transparent),
                                startX = 0f, endX = 160f
                            )
                        )
                )
                // Gradient overlay bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(60.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, AppColors.Background)
                            )
                        )
                )

                // Title text
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .padding(start = Spacing.xl, top = 4.dp)
                ) {
                    SectionLabel(str.dream_label)
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = str.dream_title1,
                        fontSize = AppType.h1,
                        fontWeight = FontWeight.Light,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = str.dream_title2,
                        fontSize = AppType.h1,
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        color = AppColors.AccentGold,
                    )
                    Spacer(Modifier.height(Spacing.s))
                    // Decorative divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier.height(1.dp).width(24.dp).background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, AppColors.AccentGold.copy(0.5f))
                                )
                            )
                        )
                        Text("✦", color = AppColors.AccentGold.copy(0.7f), fontSize = 8.sp)
                        Box(
                            Modifier.height(1.dp).width(24.dp).background(
                                Brush.horizontalGradient(
                                    listOf(AppColors.AccentGold.copy(0.5f), Color.Transparent)
                                )
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            AnimatedContent(
                targetState = state.interpretation != null,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(300))
                },
                label = "dreamContent"
            ) { hasResult ->
                if (hasResult) {
                    // ── Result view ───────────────────────────────────────────
                    ResultContent(
                        interpretation = state.interpretation ?: "",
                        onNewDream = { vm.reset() },
                    )
                } else {
                    // ── Input view ────────────────────────────────────────────
                    InputContent(
                        dreamText = state.dreamText,
                        isLoading = state.isLoading,
                        onTextChanged = vm::onDreamTextChanged,
                        onDecode = {
                            if (adManager.isAdReady) {
                                adManager.showRewardedAd(
                                    onRewarded = { vm.onAdRewarded() },
                                    onFailed   = { vm.onAdFailed(adNotReadyMsg) },
                                )
                            } else {
                                vm.onAdFailed(adNotReadyMsg)
                            }
                        },
                    )
                }
            }
        }
    }
}

// ── Input content ─────────────────────────────────────────────────────────────

@Composable
private fun InputContent(
    dreamText: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onDecode: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.xl)) {

        // ── Text input card ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.l))
                .background(AppColors.Card)
                .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.l))
                .padding(Spacing.l),
        ) {
            Column {
                // Label row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("☽", color = AppColors.AccentGold, fontSize = 14.sp)
                    Text(
                        text = str.dream_input_hint,
                        fontSize = AppType.bodyLg,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                    )
                }

                Spacer(Modifier.height(Spacing.m))

                // Text field
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp)) {
                    BasicTextField(
                        value = dreamText,
                        onValueChange = onTextChanged,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
                        textStyle = TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = AppType.body,
                            lineHeight = 22.sp,
                        ),
                        cursorBrush = SolidColor(AppColors.AccentGold),
                        decorationBox = { inner ->
                            Box {
                                if (dreamText.isEmpty()) {
                                    Text(
                                        text = str.dream_input_placeholder,
                                        color = AppColors.TextMuted,
                                        fontSize = AppType.body,
                                        lineHeight = 22.sp,
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                Spacer(Modifier.height(Spacing.s))

                // Char counter
                Text(
                    text = "${dreamText.length}${str.dream_chars_limit}",
                    fontSize = AppType.caption,
                    color = AppColors.TextDim,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }

        Spacer(Modifier.height(Spacing.l))

        // ── Decode button ─────────────────────────────────────────────────────
        val canDecode = dreamText.trim().length >= 10 && !isLoading
        DecodeButton(
            isLoading = isLoading,
            enabled = canDecode,
            onClick = onDecode,
        )

        Spacer(Modifier.height(Spacing.xl))

        // ── How it works ──────────────────────────────────────────────────────
        HowItWorksCard()

        Spacer(Modifier.height(100.dp))
    }
}

// ── Decode button ─────────────────────────────────────────────────────────────

@Composable
private fun DecodeButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val glowAlpha by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "btnGlowA",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(Radius.full))
            .background(
                if (enabled)
                    Brush.horizontalGradient(
                        listOf(
                            AppColors.AccentGold.copy(alpha = if (isLoading) 0.5f else glowAlpha),
                            Color(0xFFD4A84B).copy(alpha = if (isLoading) 0.5f else glowAlpha * 0.9f),
                        )
                    )
                else
                    Brush.horizontalGradient(
                        listOf(AppColors.AccentGoldDim.copy(0.4f), AppColors.AccentGoldDim.copy(0.3f))
                    )
            )
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                // Loading dots
                LoadingDots()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = str.dream_loading_hint,
                    fontSize = AppType.body,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Background,
                )
            } else {
                Text(
                    text = str.dream_btn_decode,
                    fontSize = AppType.body,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) AppColors.Background else AppColors.TextMuted,
                )
                if (enabled) {
                    Spacer(Modifier.width(8.dp))
                    // AD badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.Background.copy(alpha = 0.35f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = str.dream_ad_badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Background,
                        )
                    }
                }
            }
        }
    }
}

// ── Loading dots ──────────────────────────────────────────────────────────────

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = i * 150, easing = EaseInOutSine),
                    RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(AppColors.Background.copy(alpha = alpha))
            )
        }
    }
}

// ── How it works card ─────────────────────────────────────────────────────────

@Composable
private fun HowItWorksCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.l))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF12100A), Color(0xFF0C0A07))
                )
            )
            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.18f), RoundedCornerShape(Radius.l))
            .padding(Spacing.l),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            // Star icon
            Image(
                painter = painterResource(Res.drawable.ic_star),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { alpha = 0.75f }
                    .align(Alignment.Top),
            )

            Column {
                Text(
                    text = str.dream_how_title,
                    fontSize = AppType.bodyLg,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.AccentGold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = str.dream_how_desc,
                    fontSize = AppType.body,
                    color = AppColors.TextSecondary,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

// ── Result content ────────────────────────────────────────────────────────────

@Composable
private fun ResultContent(
    interpretation: String,
    onNewDream: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.xl)) {

        // Section label
        Text(
            text = str.dream_result_label,
            fontSize = AppType.caption,
            fontWeight = FontWeight.Medium,
            color = AppColors.AccentGold.copy(0.75f),
            letterSpacing = 1.2.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.m))

        // Interpretation card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.xl))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF16130E), Color(0xFF0E0C08))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            AppColors.AccentGold.copy(0.45f),
                            AppColors.AccentGold.copy(0.15f),
                            AppColors.AccentGold.copy(0.30f),
                        )
                    ),
                    RoundedCornerShape(Radius.xl),
                )
                .padding(Spacing.xl),
        ) {
            Column {
                // Moon emoji header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("☽", color = AppColors.AccentGold, fontSize = 18.sp)
                    Box(
                        Modifier.weight(1f).height(1.dp).background(
                            Brush.horizontalGradient(
                                listOf(AppColors.AccentGold.copy(0.4f), Color.Transparent)
                            )
                        )
                    )
                    Text("✦", color = AppColors.AccentGold.copy(0.5f), fontSize = 10.sp)
                }

                Spacer(Modifier.height(Spacing.m))

                Text(
                    text = interpretation,
                    fontSize = AppType.body,
                    color = AppColors.TextPrimary,
                    lineHeight = 22.sp,
                )

                Spacer(Modifier.height(Spacing.l))

                // Footer decoration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("✦", color = AppColors.AccentGold.copy(0.5f), fontSize = 10.sp)
                    Box(
                        Modifier.weight(1f).height(1.dp).background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AppColors.AccentGold.copy(0.4f))
                            )
                        )
                    )
                    Text("☾", color = AppColors.AccentGold, fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // New dream button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(Radius.full))
                .background(AppColors.Card)
                .border(1.dp, AppColors.AccentGold.copy(0.35f), RoundedCornerShape(Radius.full))
                .clickable { onNewDream() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = str.dream_btn_new,
                fontSize = AppType.body,
                fontWeight = FontWeight.Medium,
                color = AppColors.AccentGold,
            )
        }

        Spacer(Modifier.height(100.dp))
    }
}

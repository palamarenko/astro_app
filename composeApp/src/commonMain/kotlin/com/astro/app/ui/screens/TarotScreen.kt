@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.astro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.astro.app.ads.rememberAdManager
import com.astro.app.data.TarotCard
import com.astro.app.data.TarotReadingResponse
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import astroapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.*

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun TarotScreen(vm: TarotViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val adManager = rememberAdManager()
    val adNotReadyMsg = stringResource(Res.string.tarot_ad_not_ready)
    val positions = listOf(stringResource(Res.string.tarot_position_past), stringResource(Res.string.tarot_position_present), stringResource(Res.string.tarot_position_future))
    val cardTexts = listOf(state.reading?.past, state.reading?.present, state.reading?.future)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            SectionLabel(stringResource(Res.string.tarot_label))
            Spacer(Modifier.height(Spacing.m))
            Text(
                text = "${stringResource(Res.string.tarot_title1)} ${stringResource(Res.string.tarot_title2)}",
                fontSize = AppType.h1,
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(Spacing.xxl))

            Box(modifier = Modifier.fillMaxWidth()) {
                // Pulsing glow behind cards during loading
                if (state.isLoading) {
                    LoadingGlow()
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    for (i in 0..2) {
                        TarotCardSlot(
                            card = state.cards.getOrNull(i),
                            position = positions[i],
                            isRevealed = state.cards.size > i && i < state.revealedCount,
                            cardText = cardTexts.getOrNull(i),
                            cardIndex = i,
                            hasCards = state.cards.isNotEmpty(),
                            isLoading = state.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Animated loading hint below cards
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(tween(400)),
                exit  = fadeOut(tween(300))
            ) {
                TarotLoadingHint()
            }

            Spacer(Modifier.height(Spacing.xl))
            TarotDrawButton(
                canWatchAd   = state.canWatchAd,
                isLoading    = state.isLoading,
                loadingText  = stringResource(Res.string.tarot_btn_loading),
                freeText     = stringResource(Res.string.tarot_btn_open),
                adBadgeLabel = stringResource(Res.string.tarot_ad_badge),
                onClick = {
                    if (state.canWatchAd) {
                        adManager.showRewardedAd(
                            onRewarded = { vm.onAdRewarded() },
                            onFailed   = { vm.onAdFailed(adNotReadyMsg) }
                        )
                    } else {
                        vm.drawCards()
                    }
                }
            )
            // Сообщение о статусе рекламы
            AnimatedVisibility(visible = state.adMessage != null) {
                state.adMessage?.let { msg ->
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = msg,
                        fontSize = TextUnit(11f, TextUnitType.Sp),
                        color = AppColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AnimatedVisibility(
                visible = state.reading?.summary?.isNotBlank() == true,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 }
            ) {
                state.reading?.let { reading ->
                    Column {
                        Spacer(Modifier.height(Spacing.xl))
                        ReadingSummaryCard(reading = reading, cards = state.cards)
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

// ── Unified draw / ad button ──────────────────────────────────────────────────

@Composable
private fun TarotDrawButton(
    canWatchAd:   Boolean,
    isLoading:    Boolean,
    loadingText:  String,
    freeText:     String,
    adBadgeLabel: String,
    onClick:      () -> Unit,
) {
    val glowAlpha by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.9f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnGlowAlpha"
    )
    val enabled = !isLoading

    Box(
        modifier = Modifier
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Текст кнопки — всегда freeText (✦  Открыть карты  ✦) либо loadingText
            Text(
                text = if (isLoading) loadingText else freeText,
                fontSize  = AppType.body,
                fontWeight = FontWeight.Normal,
                color = if (enabled) AppColors.AccentGold else AppColors.TextDim,
                letterSpacing = TextUnit(0.08f, TextUnitType.Em),
            )

            // Плашка AD — появляется после текста когда нужна реклама
            AnimatedVisibility(
                visible = canWatchAd && !isLoading,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.7f),
                exit  = fadeOut(tween(200)) + scaleOut(tween(200)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Transparent)
                            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = adBadgeLabel,
                            fontSize = TextUnit(9f, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = AppColors.AccentGold,
                            letterSpacing = TextUnit(0.05f, TextUnitType.Em),
                        )
                    }
                }
            }
        }
    }
}

// ── Card slot ─────────────────────────────────────────────────────────────────

@Composable
private fun TarotCardSlot(
    card: TarotCard?,
    position: String,
    isRevealed: Boolean,
    cardText: String?,
    cardIndex: Int,
    hasCards: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(hasCards, card?.resourceKey) {
        if (hasCards && card != null) {
            entered = false
            kotlinx.coroutines.delay(cardIndex * 200L)
            entered = true
        } else {
            entered = false
        }
    }

    val entranceY by animateFloatAsState(
        targetValue = if (entered) 0f else -380f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "ey$cardIndex"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(220),
        label = "ea$cardIndex"
    )
    val flipRotation by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = tween(760, easing = FastOutSlowInEasing),
        label = "flip$cardIndex"
    )
    val showingFront = flipRotation > 90f

    // Shake animation while loading
    val infiniteShake = rememberInfiniteTransition(label = "shake$cardIndex")
    // Each card gets a slightly different phase offset for a natural feel
    val shakeOffset = cardIndex * 80
    val shakeX by infiniteShake.animateFloat(
        initialValue = 0f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = keyframes {
                durationMillis = 520
                0f    at 0       using LinearEasing
                (-5f) at 60      using FastOutSlowInEasing
                5f    at 160     using FastOutSlowInEasing
                (-4f) at 240     using FastOutSlowInEasing
                4f    at 320     using FastOutSlowInEasing
                (-2f) at 400     using FastOutSlowInEasing
                0f    at 460     using LinearEasing
                0f    at 520
            },
            repeatMode   = RepeatMode.Restart,
            initialStartOffset = StartOffset(shakeOffset)
        ),
        label = "shakeX$cardIndex"
    )
    val shakeRotZ by infiniteShake.animateFloat(
        initialValue = 0f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 520
                0f    at 0
                (-1.8f) at 80  using FastOutSlowInEasing
                1.8f  at 200   using FastOutSlowInEasing
                (-1.2f) at 320 using FastOutSlowInEasing
                0f    at 460
                0f    at 520
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(shakeOffset)
        ),
        label = "shakeZ$cardIndex"
    )

    Column(
        modifier = modifier.graphicsLayer {
            if (hasCards && card != null) {
                translationY = entranceY
                alpha = entranceAlpha
            }
            if (isLoading && hasCards) {
                translationX = shakeX
                rotationZ    = shakeRotZ
            }
        }
    ) {
        Text(
            text = position,
            fontSize = TextUnit(9f, TextUnitType.Sp),
            color = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .graphicsLayer {
                    rotationY = flipRotation
                    cameraDistance = 14f * density
                    // clip убран отсюда — свечение CardBack не должно обрезаться
                }
        ) {
            if (!showingFront) {
                CardBack(dimmed = !hasCards || card == null)
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Radius.m))
                    .graphicsLayer { rotationY = 180f }
                ) {
                    if (card != null) CardFront(card = card)
                }
            }
        }

        AnimatedVisibility(
            visible = isRevealed && cardText != null,
            enter = fadeIn(tween(500)) + expandVertically(tween(450))
        ) {
            if (cardText != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = cardText,
                    fontSize = TextUnit(9f, TextUnitType.Sp),
                    fontStyle = FontStyle.Italic,
                    color = AppColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = TextUnit(14f, TextUnitType.Sp)
                )
            }
        }
    }
}

// ── Mystical card back ────────────────────────────────────────────────────────

@Composable
private fun CardBack(dimmed: Boolean = false) {
    val baseAlpha = if (dimmed) 0.28f else 1f
    val gold = AppColors.AccentGold
    val breathAlpha by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0f,
        targetValue = if (dimmed) 0f else 0.18f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ba"
    )

    // Внешний Box без clip — свечение выходит за скруглённые углы
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // ── Свечение — бокс шире карты на 40dp с каждой стороны ─────────────
        if (!dimmed) {
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val extra = 40.dp.roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxWidth  = constraints.maxWidth  + extra * 2,
                                maxHeight = constraints.maxHeight + extra * 2,
                            )
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(-extra, -extra)
                        }
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(gold.copy(alpha = breathAlpha), Color.Transparent),
                            radius = 500f
                        )
                    )
            )
        }

        // ── Тело карты — клипировано по скруглённому прямоугольнику ──────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(Radius.m))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1B42).copy(alpha = baseAlpha),
                            Color(0xFF090912).copy(alpha = baseAlpha),
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = minOf(size.width, size.height) * 0.43f

                listOf(0.28f, 0.50f, 0.72f, 0.92f).forEachIndexed { idx, factor ->
                    drawCircle(
                        color = gold.copy(alpha = baseAlpha * (0.08f + idx * 0.03f)),
                        radius = maxR * factor,
                        center = Offset(cx, cy),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                val starR = maxR * 0.30f
                val innerR = starR * 0.42f
                val path = Path()
                for (i in 0 until 8) {
                    val oa = (i * 45.0 - 90.0) * (PI / 180.0)
                    val ia = ((i * 45.0 + 22.5) - 90.0) * (PI / 180.0)
                    val ox = cx + starR * cos(oa).toFloat()
                    val oy = cy + starR * sin(oa).toFloat()
                    val ix = cx + innerR * cos(ia).toFloat()
                    val iy = cy + innerR * sin(ia).toFloat()
                    if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
                    path.lineTo(ix, iy)
                }
                path.close()
                drawPath(path, color = gold.copy(alpha = baseAlpha * 0.55f), style = Fill)
                drawPath(path, color = gold.copy(alpha = baseAlpha * 0.28f), style = Stroke(0.8.dp.toPx()))
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(7.dp)
                    .border(1.dp, gold.copy(alpha = baseAlpha * 0.22f), RoundedCornerShape(Radius.m - 5.dp))
            )
            Text(text = "✦", fontSize = TextUnit(18f, TextUnitType.Sp), color = gold.copy(alpha = baseAlpha * 0.65f))
        }
    }
}

// ── Card front with real PNG image ────────────────────────────────────────────

@Composable
private fun CardFront(card: TarotCard) {
    // Reversed cards rotate 180° on Z after the flip finishes
    var doReverse by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (card.reversed) {
            kotlinx.coroutines.delay(320L) // wait for flip to nearly complete
            doReverse = true
        }
    }
    val reverseRotation by animateFloatAsState(
        targetValue = if (doReverse) 180f else 0f,
        animationSpec = tween(480, easing = FastOutSlowInEasing),
        label = "reverseZ"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        val painter = when (card.resourceKey) {
            "fool"             -> painterResource(Res.drawable.tarot_fool)
            "magician"         -> painterResource(Res.drawable.tarot_magician)
            "high_priestess"   -> painterResource(Res.drawable.tarot_high_priestess)
            "empress"          -> painterResource(Res.drawable.tarot_empress)
            "emperor"          -> painterResource(Res.drawable.tarot_emperor)
            "hierophant"       -> painterResource(Res.drawable.tarot_hierophant)
            "lovers"           -> painterResource(Res.drawable.tarot_lovers)
            "chariot"          -> painterResource(Res.drawable.tarot_chariot)
            "strength"         -> painterResource(Res.drawable.tarot_strength)
            "hermit"           -> painterResource(Res.drawable.tarot_hermit)
            "wheel_of_fortune" -> painterResource(Res.drawable.tarot_wheel_of_fortune)
            "justice"          -> painterResource(Res.drawable.tarot_justice)
            "hanged_man"       -> painterResource(Res.drawable.tarot_hanged_man)
            "death"            -> painterResource(Res.drawable.tarot_death)
            "temperance"       -> painterResource(Res.drawable.tarot_temperance)
            "devil"            -> painterResource(Res.drawable.tarot_devil)
            "tower"            -> painterResource(Res.drawable.tarot_tower)
            "star"             -> painterResource(Res.drawable.tarot_star)
            "moon"             -> painterResource(Res.drawable.tarot_moon)
            "sun"              -> painterResource(Res.drawable.tarot_sun)
            "judgment"         -> painterResource(Res.drawable.tarot_judgment)
            else               -> painterResource(Res.drawable.tarot_world)
        }

        // Only the image rotates for reversed cards
        Image(
            painter = painter,
            contentDescription = card.localizedName(),
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = reverseRotation },
            contentScale = ContentScale.Crop
        )

        // Text overlay is outside the rotating layer — always at visual bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.30f to Color.Black.copy(alpha = 0.50f),
                        1f to Color.Black.copy(alpha = 0.92f)
                    )
                ))
                .padding(horizontal = 5.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = card.number, fontSize = TextUnit(7f, TextUnitType.Sp), color = AppColors.AccentGold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                Text(text = card.localizedName(), fontSize = TextUnit(8f, TextUnitType.Sp), color = Color.White,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Loading glow behind cards ─────────────────────────────────────────────────

@Composable
private fun LoadingGlow() {
    val inf = rememberInfiniteTransition(label = "glow")
    val radius by inf.animateFloat(
        initialValue = 200f,
        targetValue  = 480f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gr"
    )
    val alpha by inf.animateFloat(
        initialValue = 0.08f,
        targetValue  = 0.22f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ga"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AppColors.AccentGold.copy(alpha = alpha),
                        Color.Transparent
                    ),
                    radius = radius
                )
            )
    )
}

// ── Animated loading hint ─────────────────────────────────────────────────────

@Composable
private fun TarotLoadingHint() {
    val inf = rememberInfiniteTransition(label = "hint")

    // Floating dots — three dots, each offset in time
    val dot1 by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)),
        label = "d1"
    )
    val dot2 by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse,
            initialStartOffset = StartOffset(300)),
        label = "d2"
    )
    val dot3 by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse,
            initialStartOffset = StartOffset(600)),
        label = "d3"
    )

    // Floating stars that drift up and fade
    val starY by inf.animateFloat(
        initialValue = 0f, targetValue = -18f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sy"
    )
    val starAlpha by inf.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sa"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Floating stars row
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.graphicsLayer { translationY = starY }
        ) {
            listOf(0.6f, 1f, 0.6f).forEachIndexed { i, scale ->
                Text(
                    text = "✦",
                    fontSize = TextUnit(10f * scale, TextUnitType.Sp),
                    color = AppColors.AccentGold.copy(alpha = starAlpha * scale),
                )
            }
        }

        // "Карты читают судьбу" + animated dots
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text = "Карты читают судьбу",
                fontSize = TextUnit(11f, TextUnitType.Sp),
                color = AppColors.TextDim,
                fontStyle = FontStyle.Italic,
                letterSpacing = TextUnit(0.5f, TextUnitType.Sp)
            )
            Spacer(Modifier.width(3.dp))
            listOf(dot1, dot2, dot3).forEach { a ->
                Text(
                    text = "·",
                    fontSize = TextUnit(14f, TextUnitType.Sp),
                    color = AppColors.AccentGold.copy(alpha = a),
                )
            }
        }
    }
}

// ── Reading summary card ──────────────────────────────────────────────────────

@Composable
private fun ReadingSummaryCard(reading: TarotReadingResponse, cards: List<TarotCard> = emptyList()) {
    // Collect all card names (Russian name + localized name) to match against summary text
    val cardNames = remember(cards) {
        cards.flatMap { card ->
            buildList {
                add(card.name)                          // Russian original, e.g. "Луна"
                // Also try common short forms for robustness
                if (card.name.contains(" ")) add(card.name.substringAfterLast(" "))
            }
        }.filter { it.length >= 3 }.distinct()
    }

    val annotated = remember(reading.summary, cardNames) {
        buildAnnotatedString {
            val text = reading.summary
            if (cardNames.isEmpty()) {
                append(text)
                return@buildAnnotatedString
            }

            // Find all match ranges (sorted, non-overlapping)
            data class Span(val start: Int, val end: Int)
            val spans = mutableListOf<Span>()
            for (name in cardNames) {
                var from = 0
                while (from < text.length) {
                    val idx = text.indexOf(name, from, ignoreCase = true)
                    if (idx == -1) break
                    // Extend to the end of the word to cover inflected forms
                    // e.g. "Дьявол" found inside "Дьявола" → bold the full "Дьявола"
                    var end = idx + name.length
                    while (end < text.length && text[end].isLetter()) end++
                    spans.add(Span(idx, end))
                    from = end
                }
            }
            // Sort and remove overlaps
            val sorted = spans.sortedBy { it.start }
            val merged = mutableListOf<Span>()
            for (s in sorted) {
                if (merged.isEmpty() || s.start >= merged.last().end) merged.add(s)
            }

            var cursor = 0
            for (span in merged) {
                if (span.start > cursor) append(text.substring(cursor, span.start))
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)) {
                    append(text.substring(span.start, span.end))
                }
                cursor = span.end
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.18f), RoundedCornerShape(Radius.m))
            .padding(18.dp)
    ) {
        Column {
            SectionLabel(stringResource(Res.string.tarot_summary_label))
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = annotated,
                fontSize = AppType.bodyLg,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = AppColors.TextSecondary,
                lineHeight = TextUnit(1.75f * 15f, TextUnitType.Sp)
            )
        }
    }
}

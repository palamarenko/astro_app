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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.astro.app.data.TarotCard
import com.astro.app.data.TarotReadingResponse
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import astroapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.*

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun TarotScreen(vm: TarotViewModel, modifier: Modifier = Modifier) {
    val s = strings()
    val state by vm.state.collectAsState()
    val positions = listOf(s.tarotPositionPast, s.tarotPositionPresent, s.tarotPositionFuture)
    val cardTexts = listOf(state.reading?.past, state.reading?.present, state.reading?.future)

    Box(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            SectionLabel(s.tarotLabel)
            Spacer(Modifier.height(Spacing.m))
            Text(text = s.tarotTitle1, fontSize = AppType.h1, fontWeight = FontWeight.Light, color = AppColors.TextPrimary)
            Text(text = s.tarotTitle2, fontSize = AppType.h1, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, color = AppColors.TextPrimary)
            Spacer(Modifier.height(Spacing.xxl))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in 0..2) {
                    TarotCardSlot(
                        card = state.cards.getOrNull(i),
                        position = positions[i],
                        isRevealed = state.cards.size > i && i < state.revealedCount,
                        cardText = cardTexts.getOrNull(i),
                        cardIndex = i,
                        hasCards = state.cards.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            GoldButton(
                text = if (state.isLoading) s.tarotBtnLoading else s.tarotBtnOpen,
                onClick = { vm.drawCards() },
                enabled = !state.isLoading
            )

            AnimatedVisibility(
                visible = state.reading?.summary?.isNotBlank() == true,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 }
            ) {
                state.reading?.let { reading ->
                    Column {
                        Spacer(Modifier.height(Spacing.xl))
                        ReadingSummaryCard(reading = reading, s = s)
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
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

    Column(
        modifier = modifier.graphicsLayer {
            if (hasCards && card != null) {
                translationY = entranceY
                alpha = entranceAlpha
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
                    shape = RoundedCornerShape(Radius.m)
                    clip = true
                }
        ) {
            if (!showingFront) {
                CardBack(dimmed = !hasCards || card == null)
            } else {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
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
        targetValue = if (dimmed) 0f else 0.13f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ba"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(
                colors = listOf(Color(0xFF1E1B42).copy(alpha = baseAlpha), Color(0xFF090912).copy(alpha = baseAlpha))
            )),
        contentAlignment = Alignment.Center
    ) {
        if (!dimmed) {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(colors = listOf(gold.copy(alpha = breathAlpha), Color.Transparent), radius = 380f)
            ))
        }

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

// ── Card front with real PNG image ────────────────────────────────────────────

@Composable
private fun CardFront(card: TarotCard) {
    val s = strings()

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

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = reverseRotation }) {

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

        Image(
            painter = painter,
            contentDescription = card.localizedName(s),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
                Text(text = card.localizedName(s), fontSize = TextUnit(8f, TextUnitType.Sp), color = Color.White,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ── Reading summary card ──────────────────────────────────────────────────────

@Composable
private fun ReadingSummaryCard(reading: TarotReadingResponse, s: StringBundle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.18f), RoundedCornerShape(Radius.m))
            .padding(18.dp)
    ) {
        Column {
            SectionLabel(s.tarotSummaryLabel)
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = reading.summary,
                fontSize = AppType.bodyLg,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = AppColors.TextSecondary,
                lineHeight = TextUnit(1.75f * 15f, TextUnitType.Sp)
            )
        }
    }
}

package com.astro.app.ui.screens.horoscope

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import astroapp.composeapp.generated.resources.*
import com.astro.app.ads.AdManager
import com.astro.app.ads.rememberAdManager
import com.astro.app.data.ALL_SIGNS
import com.astro.app.data.HoroscopePeriod
import com.astro.app.data.HoroscopeResponse
import com.astro.app.data.ZodiacSign
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Image
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.*

// ── Score colours matching the spec ──────────────────────────────────────────
private val ScoreLove    = Color(0xFFE85D8A)
private val ScoreCareer  = AppColors.AccentGold
private val ScoreHealth  = Color(0xFF8AAB7A)
private val ScoreEnergy  = Color(0xFF7EC8E3)


// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun HoroscopeScreen(vm: HoroscopeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()
    val sign = state.selectedSign ?: return
    val elementColor = AppColors.elementColor(sign.element)
    val adManager = rememberAdManager()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
        ) {
            Spacer(Modifier.height(Spacing.xxl))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(elementColor.copy(alpha = 0.10f))
                        .border(1.dp, elementColor.copy(alpha = 0.28f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 18.sp, color = elementColor)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = sign.localizedDates().uppercase(),
                    fontSize = AppType.caption,
                    color = AppColors.AccentGold,
                    letterSpacing = TextUnit(0.22f, TextUnitType.Em),
                )
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(40.dp))
            }

            Spacer(Modifier.height(Spacing.xxl))

            // ── Cosmic Hero ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CosmicHero(sign = sign, elementColor = elementColor)
            }

            Spacer(Modifier.height(Spacing.l))

            // ── Sign name + pills ─────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = sign.localizedName(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = AppColors.TextPrimary,
                    letterSpacing = TextUnit(0.01f, TextUnitType.Em),
                )
                Spacer(Modifier.height(Spacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElementPill(sign.localizedElement(), elementColor)
                    ElementPill(sign.localizedPlanet(), AppColors.AccentGold)
                }
            }

            Spacer(Modifier.height(Spacing.l))

            // ── Tabs ──────────────────────────────────────────────────────────
            PeriodTabsNew(
                selected  = state.period,
                onSelect  = { vm.setPeriod(it) },
                elementColor = elementColor,
            )

            Spacer(Modifier.height(14.dp))

            // ── Current forecast ──────────────────────────────────────────────
            AnimatedContent(
                targetState = Triple(state.period, state.loadingCurrent, state.currentForecast),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "forecast"
            ) { (_, loading, forecast) ->
                when {
                    loading   -> LoadingPlaceholder()
                    forecast != null -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ForecastCard(forecast = forecast, elementColor = elementColor)
                            ScoreGaugesRow(forecast = forecast)
                        }
                    }
                    else -> LoadingPlaceholder()
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Wizard CTA or future forecast ─────────────────────────────────
            AnimatedContent(
                targetState = Triple(state.period, state.isUnlocked, state.futureForecast),
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
                label = "future"
            ) { (period, unlocked, future) ->
                if (!unlocked) {
                    // Show wizard CTA
                    WizardCta(
                        period    = period,
                        onClick   = { vm.showWizard() },
                        elementColor = elementColor,
                    )
                } else {
                    // Show future forecast
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FutureDivider(period = period, elementColor = elementColor)
                        if (state.loadingFuture || future == null) {
                            LoadingPlaceholder()
                        } else {
                            ForecastCard(
                                forecast     = future,
                                elementColor = elementColor,
                                isFuture     = true,
                            )
                            ScoreGaugesRow(forecast = future)
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // ── Wizard Modal overlay ──────────────────────────────────────────────
        if (state.showWizard) {
            WizardModal(
                period       = state.period,
                sign         = sign,
                elementColor = elementColor,
                adManager    = adManager,
                onDismiss    = { vm.dismissWizard() },
                onComplete   = { vm.unlockAndLoadFuture(state.period) },
            )
        }
    }
}

// ── CosmicHero ────────────────────────────────────────────────────────────────

@Composable
private fun CosmicHero(sign: ZodiacSign, elementColor: Color) {
    val inf = rememberInfiniteTransition(label = "hero")

    val outerRot by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(24000, easing = LinearEasing)), "outerRot")
    val innerRot by inf.animateFloat(0f, -360f,
        infiniteRepeatable(tween(18000, easing = LinearEasing)), "innerRot")
    val auraScale by inf.animateFloat(1f, 1.08f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "auraS")
    val auraAlpha by inf.animateFloat(0.55f, 0.95f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "auraA")
    val symbolFloat by inf.animateFloat(0f, -4f,
        infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "float")

    // 6 sparkle particles with stagger
    val sp0 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse, StartOffset(0)), "sp0")
    val sp1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse, StartOffset(800)), "sp1")
    val sp2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse, StartOffset(1600)), "sp2")
    val sp3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse, StartOffset(2400)), "sp3")
    val sp4 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse, StartOffset(3200)), "sp4")
    val sp5 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse, StartOffset(4000)), "sp5")
    val sparkles = listOf(sp0, sp1, sp2, sp3, sp4, sp5)

    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Aura pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        elementColor.copy(alpha = 0.28f * auraAlpha),
                        elementColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = cx * auraScale,
                ),
                radius = cx * auraScale,
                center = Offset(cx, cy),
            )

            // Core glow disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(elementColor.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 55.dp.toPx(),
                ),
                radius = 55.dp.toPx(),
                center = Offset(cx, cy),
            )

            val outerR = 82.dp.toPx()
            val innerR = 52.dp.toPx()

            // Outer ring — dashed circle
            rotate(outerRot, pivot = Offset(cx, cy)) {
                drawCircle(
                    color  = AppColors.AccentGold.copy(alpha = 0.35f),
                    radius = outerR,
                    center = Offset(cx, cy),
                    style  = Stroke(
                        width      = 0.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), 6.dp.toPx())
                        )
                    )
                )
                // 12 dots on outer ring
                for (i in 0..11) {
                    val angle  = (i * 30.0) * PI / 180.0
                    val dotX   = cx + outerR * cos(angle).toFloat()
                    val dotY   = cy + outerR * sin(angle).toFloat()
                    val cardinal = i % 3 == 0
                    drawCircle(
                        color  = AppColors.AccentGold.copy(alpha = if (cardinal) 0.85f else 0.35f),
                        radius = if (cardinal) 1.8.dp.toPx() else 1.dp.toPx(),
                        center = Offset(dotX, dotY),
                    )
                }
            }

            // Inner ring — 3 planets
            rotate(innerRot, pivot = Offset(cx, cy)) {
                drawCircle(
                    color  = AppColors.AccentGold.copy(alpha = 0.18f),
                    radius = innerR,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 1.dp.toPx())
                )
                for (i in 0..2) {
                    val angle  = (i * 120.0) * PI / 180.0
                    val dotX   = cx + innerR * cos(angle).toFloat()
                    val dotY   = cy + innerR * sin(angle).toFloat()
                    drawCircle(
                        color  = if (i == 0) elementColor else AppColors.AccentGold,
                        radius = 2.6.dp.toPx(),
                        center = Offset(dotX, dotY),
                    )
                }
            }

            // Sparkle particles
            val sparklePts = listOf(
                Offset(cx + 76.dp.toPx(), cy - 42.dp.toPx()),
                Offset(cx - 72.dp.toPx(), cy + 28.dp.toPx()),
                Offset(cx + 48.dp.toPx(), cy + 72.dp.toPx()),
                Offset(cx - 60.dp.toPx(), cy - 56.dp.toPx()),
                Offset(cx + 18.dp.toPx(), cy - 82.dp.toPx()),
                Offset(cx - 28.dp.toPx(), cy + 80.dp.toPx()),
            )
            sparklePts.forEachIndexed { i, pt ->
                val a = sparkles[i]
                if (a > 0.05f) {
                    drawCircle(
                        color  = AppColors.AccentGold,
                        radius = 1.5.dp.toPx(),
                        center = pt,
                        alpha  = a,
                    )
                }
            }
        }

        // Floating zodiac icon
        Image(
            painter            = sign.iconPainter(),
            contentDescription = sign.localizedName(),
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .size(88.dp)
                .graphicsLayer { translationY = symbolFloat * density },
        )
    }
}

// ── Period tabs (new style) ───────────────────────────────────────────────────

@Composable
private fun PeriodTabsNew(
    selected: HoroscopePeriod,
    onSelect: (HoroscopePeriod) -> Unit,
    elementColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E0E18))
            .border(1.dp, Color(0xFF1A1A26), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HoroscopePeriod.entries.forEach { period ->
            val isActive = period == selected
            val bgColor by animateColorAsState(
                targetValue = if (isActive) AppColors.AccentGold.copy(alpha = 0.12f) else Color.Transparent,
                animationSpec = tween(200), label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) AppColors.AccentGold else Color(0xFF666666),
                animationSpec = tween(200), label = "tabTxt"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bgColor)
                    .then(
                        if (isActive) Modifier.border(
                            1.dp,
                            AppColors.AccentGold.copy(alpha = 0.27f),
                            RoundedCornerShape(9.dp)
                        ) else Modifier
                    )
                    .clickable { onSelect(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = period.localizedLabel(),
                    fontSize   = 11.sp,
                    color      = textColor,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

// ── Forecast card ─────────────────────────────────────────────────────────────

@Composable
private fun ForecastCard(
    forecast: HoroscopeResponse,
    elementColor: Color,
    isFuture:     Boolean = false,
) {
    val borderColor = if (isFuture) AppColors.AccentGold.copy(alpha = 0.35f) else Color(0xFF1D1D29)
    val bgBrush = if (isFuture)
        Brush.linearGradient(
            colors = listOf(elementColor.copy(alpha = 0.06f), Color(0xFF101019)),
        )
    else
        Brush.linearGradient(
            colors = listOf(Color(0xFF15151F), Color(0xFF101019)),
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        // Decorative opening quote (top-left, faint)
        Text(
            text     = "“",
            fontSize = 54.sp,
            color    = AppColors.AccentGold.copy(alpha = 0.12f),
            modifier = Modifier.align(Alignment.TopStart).offset(y = (-10).dp),
            fontStyle = FontStyle.Italic,
        )
        // Decorative closing quote (bottom-right, faint)
        Text(
            text     = "”",
            fontSize = 54.sp,
            color    = AppColors.AccentGold.copy(alpha = 0.12f),
            modifier = Modifier.align(Alignment.BottomEnd).offset(y = 10.dp),
            fontStyle = FontStyle.Italic,
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // Keyword divider
            if (forecast.keyword.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth(),
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(AppColors.AccentGold.copy(alpha = 0.18f)))
                    Text(
                        text          = "  ${forecast.keyword.uppercase()}  ",
                        fontSize      = 10.sp,
                        color         = AppColors.AccentGold,
                        fontWeight    = FontWeight.Normal,
                        letterSpacing = TextUnit(0.28f, TextUnitType.Em),
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(AppColors.AccentGold.copy(alpha = 0.18f)))
                }
                Spacer(Modifier.height(14.dp))
            }

            // Forecast text
            Text(
                text       = forecast.text,
                fontSize   = 15.5.sp,
                fontStyle  = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color      = Color(0xFFD8D0C0),
                lineHeight = TextUnit(26f, TextUnitType.Sp),
            )
        }
    }
}

// ── Score gauges row (4 circular) ─────────────────────────────────────────────

@Composable
private fun ScoreGaugesRow(forecast: HoroscopeResponse) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ScoreGauge(value = forecast.love,    label = stringResource(Res.string.horoscope_score_love),    icon = "♡", color = ScoreLove)
        ScoreGauge(value = forecast.career,  label = stringResource(Res.string.horoscope_score_career),  icon = "✦", color = ScoreCareer)
        ScoreGauge(value = forecast.health,  label = stringResource(Res.string.horoscope_score_health),  icon = "◎", color = ScoreHealth)
        ScoreGauge(value = forecast.energy,  label = stringResource(Res.string.horoscope_score_energy),  icon = "⊕", color = ScoreEnergy)
    }
}

@Composable
private fun ScoreGauge(value: Int, label: String, icon: String, color: Color) {
    val sweepAngle by animateFloatAsState(
        targetValue    = (value / 100f) * 360f,
        animationSpec  = tween(1200, easing = FastOutSlowInEasing),
        label          = "gauge_$label"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier        = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 3.dp.toPx()
                val inset   = strokeW / 2f
                val r       = size.minDimension / 2f - inset

                // Background track
                drawCircle(
                    color  = color.copy(alpha = 0.12f),
                    radius = r,
                    style  = Stroke(width = strokeW),
                )
                // Progress arc
                drawArc(
                    color       = color,
                    startAngle  = -90f,
                    sweepAngle  = sweepAngle,
                    useCenter   = false,
                    style       = Stroke(width = strokeW, cap = StrokeCap.Round),
                    alpha       = 0.9f,
                )
                // Glow drop-shadow effect (second arc, blurred via alpha)
                drawArc(
                    color       = color.copy(alpha = 0.25f),
                    startAngle  = -90f,
                    sweepAngle  = sweepAngle,
                    useCenter   = false,
                    style       = Stroke(width = strokeW * 3, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, fontSize = 9.sp, color = color)
                Text(
                    text       = value.toString(),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Light,
                    color      = AppColors.TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text          = label,
            fontSize      = 10.sp,
            color         = AppColors.TextMuted,
            letterSpacing = TextUnit(0.06f, TextUnitType.Em),
        )
    }
}

// ── Wizard CTA ────────────────────────────────────────────────────────────────

@Composable
private fun WizardCta(
    period:       HoroscopePeriod,
    onClick:      () -> Unit,
    elementColor: Color,
) {
    val periodAcc = when (period) {
        HoroscopePeriod.DAILY   -> stringResource(Res.string.wizard_period_acc_daily)
        HoroscopePeriod.WEEKLY  -> stringResource(Res.string.wizard_period_acc_week)
        HoroscopePeriod.MONTHLY -> stringResource(Res.string.wizard_period_acc_month)
    }
    val inf    = rememberInfiniteTransition(label = "ctaWiz")
    val floatY by inf.animateFloat(-2f, 2f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "wizY")
    val glowA  by inf.animateFloat(0.4f, 0.8f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "wizG")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(
                listOf(AppColors.AccentGold.copy(alpha = 0.07f), Color(0xFF101019))
            ))
            .border(1.dp, AppColors.AccentGold.copy(alpha = glowA * 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter            = painterResource(Res.drawable.w),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .size(44.dp)
                    .graphicsLayer { translationY = floatY * density },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = stringResource(Res.string.wizard_cta_title, periodAcc),
                    fontSize   = 15.sp,
                    fontStyle  = FontStyle.Italic,
                    color      = AppColors.AccentGold,
                    fontWeight = FontWeight.Normal,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = stringResource(Res.string.wizard_cta_desc),
                    fontSize = 12.sp,
                    color    = AppColors.TextMuted,
                )
            }
            Text("→", fontSize = 18.sp, color = AppColors.AccentGold.copy(alpha = 0.7f))
        }
    }
}

// ── Future section divider ────────────────────────────────────────────────────

@Composable
private fun FutureDivider(period: HoroscopePeriod, elementColor: Color) {
    val label = when (period) {
        HoroscopePeriod.DAILY   -> stringResource(Res.string.wizard_divider_daily)
        HoroscopePeriod.WEEKLY  -> stringResource(Res.string.wizard_divider_week)
        HoroscopePeriod.MONTHLY -> stringResource(Res.string.wizard_divider_month)
    }
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(AppColors.AccentGold.copy(alpha = 0.2f)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Image(
                painter            = painterResource(Res.drawable.w),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.size(14.dp),
            )
            Text(
                text          = label,
                fontSize      = 10.sp,
                color         = AppColors.AccentGold,
                letterSpacing = TextUnit(0.14f, TextUnitType.Em),
            )
        }
        Box(Modifier.weight(1f).height(1.dp).background(AppColors.AccentGold.copy(alpha = 0.2f)))
    }
}

// ── Loading placeholder ───────────────────────────────────────────────────────

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier        = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center
    ) { LoadingDots() }
}

// ── Wizard Modal ──────────────────────────────────────────────────────────────

@Composable
private fun WizardModal(
    period:       HoroscopePeriod,
    sign:         ZodiacSign,
    elementColor: Color,
    adManager:    AdManager,
    onDismiss:    () -> Unit,
    onComplete:   () -> Unit,
) {
    val periodAcc = when (period) {
        HoroscopePeriod.DAILY   -> stringResource(Res.string.wizard_period_acc_daily)
        HoroscopePeriod.WEEKLY  -> stringResource(Res.string.wizard_period_acc_week)
        HoroscopePeriod.MONTHLY -> stringResource(Res.string.wizard_period_acc_month)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF1A1525), Color(0xFF0D0D18))
                    ))
                    .border(1.dp, AppColors.AccentGold.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                WizardIntro(
                    periodAcc    = periodAcc,
                    signName     = sign.localizedName(),
                    elementColor = elementColor,
                    onWatch      = {
                        // Всегда запускаем реальную рекламу AdMob
                        adManager.showRewardedAd(
                            onRewarded = { onComplete() },
                            // Если реклама не готова — всё равно даём доступ
                            onFailed   = { onComplete() },
                        )
                    },
                    onDismiss    = onDismiss,
                )
            }
        }
    }
}

// ── Wizard phases ─────────────────────────────────────────────────────────────

@Composable
private fun WizardIntro(
    periodAcc:    String,
    signName:     String,
    elementColor: Color,
    onWatch:      () -> Unit,
    onDismiss:    () -> Unit,
) {
    val inf    = rememberInfiniteTransition(label = "wizI")
    val floatY by inf.animateFloat(-5f, 5f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "wY")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter            = painterResource(Res.drawable.w),
            contentDescription = null,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .size(120.dp)
                .graphicsLayer { translationY = floatY * density },
        )
        Text(
            text      = stringResource(Res.string.wizard_intro_title, periodAcc),
            fontSize  = 22.sp,
            fontStyle = FontStyle.Italic,
            color     = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
        )
        Text(
            text      = stringResource(Res.string.wizard_intro_subtitle, signName, periodAcc),
            fontSize  = 13.sp,
            color     = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        // Primary CTA button
        val glowAlpha by rememberInfiniteTransition(label = "btnG").animateFloat(
            0.55f, 1f,
            infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bGA"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppColors.AccentGold.copy(alpha = 0.18f),
                            AppColors.AccentGold.copy(alpha = 0.10f),
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            AppColors.AccentGold.copy(alpha = glowAlpha),
                            AppColors.AccentGold.copy(alpha = glowAlpha * 0.6f),
                        )
                    ),
                    RoundedCornerShape(14.dp)
                )
                .clickable { onWatch() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text          = stringResource(Res.string.wizard_btn_watch),
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = AppColors.AccentGold,
                    letterSpacing = TextUnit(0.08f, TextUnitType.Em),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, AppColors.AccentGold.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = stringResource(Res.string.tarot_ad_badge),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AppColors.AccentGold,
                        letterSpacing = TextUnit(0.05f, TextUnitType.Em),
                    )
                }
            }
        }
        // Not now
        Text(
            text      = stringResource(Res.string.wizard_btn_not_now),
            fontSize  = 12.sp,
            color     = Color(0xFF666666),
            modifier  = Modifier.clickable { onDismiss() }.padding(8.dp),
            textAlign = TextAlign.Center,
        )
    }
}


// ── Previews ──────────────────────────────────────────────────────────────────

private val PreviewSign = ALL_SIGNS.first { it.id == "cancer" }
private val PreviewForecast = HoroscopeResponse(
    text    = "Сегодня звёзды дарят особую чуткость — прислушайся к сердцу и не бойся действовать по интуиции. Близкие окажут неожиданную поддержку, а маленькая победа в делах поднимет настроение на весь день.",
    keyword = "Интуиция",
    love    = 82, career = 88, health = 64, energy = 75,
)

@Preview
@Composable
fun PreviewCosmicHero() {
    Box(Modifier.background(AppColors.Background)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Background)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            CosmicHero(sign = PreviewSign, elementColor = AppColors.elementColor(PreviewSign.element))
        }
    }
}

@Preview
@Composable
fun PreviewForecastCard() {
    Box(Modifier.background(AppColors.Background)) {
        Column(
            modifier = Modifier
                .background(AppColors.Background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ForecastCard(forecast = PreviewForecast, elementColor = AppColors.elementColor(PreviewSign.element))
            ScoreGaugesRow(forecast = PreviewForecast)
        }
    }
}

@Preview
@Composable
fun PreviewWizardCta() {
    Box(Modifier.background(AppColors.Background)) {
        Column(
            modifier = Modifier
                .background(AppColors.Background)
                .padding(16.dp),
        ) {
            WizardCta(
                period       = HoroscopePeriod.DAILY,
                onClick      = {},
                elementColor = AppColors.elementColor(PreviewSign.element),
            )
        }
    }
}

@Preview
@Composable
fun PreviewWizardIntroPhase() {
    Box(Modifier.background(AppColors.Background)) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A1525), Color(0xFF0D0D18))))
                    .border(1.dp, AppColors.AccentGold.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                WizardIntro(
                    periodAcc    = "на завтра",
                    signName     = PreviewSign.name,
                    elementColor = AppColors.elementColor(PreviewSign.element),
                    onWatch      = {},
                    onDismiss    = {},
                )
            }
        }
    }
}


@Preview
@Composable
fun PreviewHoroscopeMain() {
    Box(Modifier.background(AppColors.Background)) {
        val elementColor = AppColors.elementColor(PreviewSign.element)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CosmicHero(sign = PreviewSign, elementColor = elementColor)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(PreviewSign.name, fontSize = 36.sp, fontWeight = FontWeight.Light, color = AppColors.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElementPill(PreviewSign.element, elementColor)
                    ElementPill(PreviewSign.planet, AppColors.AccentGold)
                }
            }
            PeriodTabsNew(selected = HoroscopePeriod.DAILY, onSelect = {}, elementColor = elementColor)
            ForecastCard(forecast = PreviewForecast, elementColor = elementColor)
            ScoreGaugesRow(forecast = PreviewForecast)
            WizardCta(period = HoroscopePeriod.DAILY, onClick = {}, elementColor = elementColor)
            Spacer(Modifier.height(16.dp))
        }
    }
}

package com.astro.app.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.astro.app.data.HoroscopePeriod
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import com.astro.app.viewmodel.HoroscopeViewModel

@Composable
fun HoroscopeScreen(vm: HoroscopeViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val s = strings()
    val state by vm.state.collectAsState()
    val sign = state.selectedSign ?: return
    val elementColor = AppColors.elementColor(sign.element)

    Box(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl)
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            // Top bar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = s.horoscopeBack, fontSize = TextUnit(22f, TextUnitType.Sp), color = AppColors.TextMuted,
                    modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.weight(1f))
                Text(text = sign.localizedDates(s).uppercase(), fontSize = AppType.caption, color = AppColors.AccentGold,
                    letterSpacing = TextUnit(0.22f, TextUnitType.Em))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(28.dp))
            }
            Spacer(Modifier.height(Spacing.xxl))
            // Hero
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(76.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(elementColor.copy(alpha = 0.15f), AppColors.Background)))
                        .border(1.dp, elementColor.copy(alpha = 0.27f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(text = sign.emoji, fontSize = TextUnit(30f, TextUnitType.Sp)) }
                Spacer(Modifier.height(Spacing.m))
                Text(text = sign.localizedName(s), fontSize = AppType.h2, fontWeight = FontWeight.Light, color = AppColors.TextPrimary)
                Spacer(Modifier.height(Spacing.s))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElementPill(sign.localizedElement(s), elementColor)
                    ElementPill(sign.localizedPlanet(s), AppColors.AccentGold)
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            PeriodTabs(selected = state.period, onSelect = { vm.setPeriod(it) })
            Spacer(Modifier.height(Spacing.l))
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                label = "horoscope"
            ) { st ->
                when {
                    st.isLoading -> Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { LoadingDots() }
                    st.horoscope != null -> {
                        val h = st.horoscope
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m)).background(AppColors.Card).padding(18.dp)) {
                                Column {
                                    SectionLabel("text")
                                    Spacer(Modifier.height(Spacing.s))
                                    Text(text = h.text, fontSize = AppType.bodyLg, fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Light, color = AppColors.TextSecondary,
                                        lineHeight = TextUnit(1.75f * 15, TextUnitType.Sp))
                                }
                            }
                            Spacer(Modifier.height(Spacing.m))
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                                    ScoreCard(label = s.horoscopeScoreLove, icon = "♡", score = h.love, color = AppColors.Fire, modifier = Modifier.weight(1f))
                                    ScoreCard(label = s.horoscopeScoreCareer, icon = "◈", score = h.career, color = AppColors.AccentGold, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                                    ScoreCard(label = s.horoscopeScoreHealth, icon = "✦", score = h.health, color = AppColors.Earth, modifier = Modifier.weight(1f))
                                    ScoreCard(label = s.horoscopeScoreEnergy, icon = "◎", score = h.energy, color = AppColors.Air, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    st.error != null -> Text(text = s.horoscopeError(st.error), color = AppColors.Fire,
                        fontSize = AppType.body, modifier = Modifier.padding(vertical = 16.dp))
                    else -> Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text(text = s.horoscopeSelectPeriod, color = AppColors.TextMuted, fontSize = AppType.body)
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PeriodTabs(selected: HoroscopePeriod, onSelect: (HoroscopePeriod) -> Unit) {
    val s = strings()
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m)).background(AppColors.Card).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HoroscopePeriod.entries.forEach { period ->
            val isActive = period == selected
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s))
                    .background(if (isActive) AppColors.Surface else AppColors.Card)
                    .clickable { onSelect(period) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = period.localizedLabel(s), fontSize = AppType.body,
                    color = if (isActive) AppColors.AccentGold else AppColors.TextDim,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal)
            }
        }
    }
}

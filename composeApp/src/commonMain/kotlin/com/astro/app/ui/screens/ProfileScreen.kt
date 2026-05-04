package com.astro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import com.astro.app.data.ALL_SIGNS
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import com.astro.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(vm: ProfileViewModel, modifier: Modifier = Modifier) {
    val s = strings()
    val state by vm.state.collectAsState()
    val sign = state.sign
    val elementColor = AppColors.elementColor(sign.element)

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowA"
    )

    Box(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            // Avatar
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(elementColor.copy(alpha = glowAlpha * 0.3f), AppColors.Background)))
                    .border(1.dp, Brush.linearGradient(listOf(elementColor.copy(alpha = glowAlpha), elementColor.copy(alpha = glowAlpha * 0.4f))), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(text = sign.emoji, fontSize = TextUnit(32f, TextUnitType.Sp)) }

            Spacer(Modifier.height(Spacing.m))
            Text(text = sign.localizedName(s), fontSize = AppType.h2, fontWeight = FontWeight.Light, color = AppColors.TextPrimary)
            Text(text = sign.localizedDates(s), fontSize = AppType.caption, color = AppColors.TextDim)
            Spacer(Modifier.height(Spacing.m))

            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.full))
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.27f), RoundedCornerShape(Radius.full))
                .clickable { vm.toggleSignPicker() }.padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(text = if (state.showSignPicker) s.profileBtnClose else s.profileBtnChange,
                    fontSize = AppType.caption, color = AppColors.AccentGold)
            }

            AnimatedVisibility(visible = state.showSignPicker,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
                Column {
                    Spacer(Modifier.height(Spacing.m))
                    LazyVerticalGrid(columns = GridCells.Fixed(6), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp), userScrollEnabled = false) {
                        items(ALL_SIGNS.size) { i ->
                            val sg = ALL_SIGNS[i]; val isSel = sg == sign
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.s))
                                .background(if (isSel) AppColors.elementColor(sg.element).copy(alpha = 0.15f) else AppColors.Card)
                                .border(1.dp, if (isSel) AppColors.elementColor(sg.element).copy(alpha = 0.4f) else AppColors.Border, RoundedCornerShape(Radius.s))
                                .clickable { vm.selectSign(sg) }.padding(6.dp), contentAlignment = Alignment.Center) {
                                Text(text = sg.emoji, fontSize = TextUnit(18f, TextUnitType.Sp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m)).background(AppColors.Card),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(s.profileStatElement, sign.localizedElement(s), elementColor)
                Box(modifier = Modifier.width(1.dp).height(48.dp).background(AppColors.Border).align(Alignment.CenterVertically))
                StatItem(s.profileStatPlanet, sign.localizedPlanet(s), AppColors.AccentGold)
                Box(modifier = Modifier.width(1.dp).height(48.dp).background(AppColors.Border).align(Alignment.CenterVertically))
                StatItem(s.profileStatPeriod, sign.localizedDates(s).split("–").first().trim(), AppColors.TextMuted)
            }

            Spacer(Modifier.height(Spacing.xl))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m)).background(AppColors.Card).padding(18.dp)) {
                Column {
                    SectionLabel(s.profileInsightLabel)
                    Spacer(Modifier.height(Spacing.s))
                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { LoadingDots() }
                    } else {
                        Text(text = state.insight ?: s.profileInsightLoading, fontSize = AppType.bodyLg,
                            fontStyle = FontStyle.Italic, fontWeight = FontWeight.Light, color = AppColors.TextSecondary,
                            lineHeight = TextUnit(1.75f * 15, TextUnitType.Sp))
                    }
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = AppType.body, color = color, fontWeight = FontWeight.Normal)
        Text(text = label, fontSize = TextUnit(9f, TextUnitType.Sp), color = AppColors.TextDim)
    }
}

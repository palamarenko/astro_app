package com.astro.app.ui.screens.compatibility

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import astroapp.composeapp.generated.resources.*
import com.astro.app.data.ALL_SIGNS
import com.astro.app.data.ZodiacSign
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CompatibilityScreen(vm: CompatibilityViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl)) {
            Spacer(Modifier.height(Spacing.xxl))
            SectionLabel(stringResource(Res.string.compat_label))
            Spacer(Modifier.height(Spacing.m))
            Text(text = stringResource(Res.string.compat_title1), fontSize = AppType.h1, fontWeight = FontWeight.Light, color = AppColors.TextPrimary)
            Text(text = stringResource(Res.string.compat_title2), fontSize = AppType.h1, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, color = AppColors.TextPrimary)
            Spacer(Modifier.height(Spacing.xl))

            Text(text = stringResource(Res.string.compat_sign1), fontSize = AppType.caption, color = AppColors.TextMuted)
            Spacer(Modifier.height(Spacing.s))
            SignRow(selected = state.sign1, onSelect = { vm.setSign1(it) })
            Spacer(Modifier.height(Spacing.xl))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColors.Border))
                Text(text = "  ✦  ", color = AppColors.AccentGold.copy(alpha = 0.5f), fontSize = AppType.body)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(AppColors.Border))
            }
            Spacer(Modifier.height(Spacing.xl))

            Text(text = stringResource(Res.string.compat_sign2), fontSize = AppType.caption, color = AppColors.TextMuted)
            Spacer(Modifier.height(Spacing.s))
            SignRow(selected = state.sign2, onSelect = { if (state.sign1 != null) vm.setSign2(it) }, enabled = state.sign1 != null)
            Spacer(Modifier.height(Spacing.xl))

            AnimatedContent(targetState = state,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) }, label = "compat") { st ->
                when {
                    st.isLoading -> Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { LoadingDots() }
                    st.error != null -> Text(
                        text = stringResource(Res.string.compat_error, st.error),
                        color = AppColors.Fire.copy(alpha = 0.8f), fontSize = AppType.body,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center)
                    st.result != null -> {
                        val r = st.result
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m))
                                .background(AppColors.Card).padding(20.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "${r.score}", fontSize = TextUnit(48f, TextUnitType.Sp),
                                        fontWeight = FontWeight.Light, color = AppColors.AccentGold)
                                    Text(text = r.title, fontSize = AppType.title, fontWeight = FontWeight.Light,
                                        color = AppColors.TextPrimary, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(Spacing.m))
                                    Text(text = r.text, fontSize = AppType.bodyLg, fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Light, color = AppColors.TextSecondary,
                                        textAlign = TextAlign.Center, lineHeight = TextUnit(1.75f * 15, TextUnitType.Sp))
                                }
                            }
                            Spacer(Modifier.height(Spacing.m))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                InfoCard(stringResource(Res.string.compat_strengths), r.strengths, AppColors.Earth, Modifier.weight(1f))
                                InfoCard(stringResource(Res.string.compat_challenges), r.challenges, AppColors.Fire, Modifier.weight(1f))
                            }
                        }
                    }
                    else -> Text(
                        text = if (st.sign1 == null) stringResource(Res.string.compat_select_sign1) else stringResource(Res.string.compat_select_sign2),
                        color = AppColors.TextMuted, fontSize = AppType.body,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SignRow(selected: ZodiacSign?, onSelect: (ZodiacSign) -> Unit, enabled: Boolean = true) {
    LazyHorizontalGrid(rows = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().height(88.dp), contentPadding = PaddingValues(vertical = 0.dp)) {
        items(ALL_SIGNS.size) { i ->
            val sign = ALL_SIGNS[i]; val isSelected = sign == selected
            val elementColor = AppColors.elementColor(sign.element)
            Box(modifier = Modifier.width(56.dp).fillMaxHeight().clip(RoundedCornerShape(Radius.s))
                .background(if (isSelected) elementColor.copy(alpha = 0.12f) else AppColors.Card)
                .border(1.dp, if (isSelected) elementColor.copy(alpha = 0.5f) else AppColors.Border, RoundedCornerShape(Radius.s))
                .clickable(enabled = enabled) { onSelect(sign) }.padding(4.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = sign.iconPainter(),
                        contentDescription = sign.localizedName(),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(text = sign.localizedName(), fontSize = TextUnit(8f, TextUnitType.Sp),
                        color = if (isSelected) elementColor else AppColors.TextDim)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(Radius.m)).background(AppColors.Card)
        .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(Radius.m)).padding(14.dp)) {
        Column {
            Text(text = title.uppercase(), fontSize = AppType.label, color = color)}}}
               
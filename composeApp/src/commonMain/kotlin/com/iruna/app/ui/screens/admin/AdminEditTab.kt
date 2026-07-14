package com.iruna.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.data.*
import com.iruna.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// EDIT TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun EditTab(state: AdminUiState, vm: AdminViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(top = Spacing.l)) {

                ControlLabel("LANGUAGE")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALL_GEN_LANG_LABELS.chunked(3).forEach { rowLangs ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowLangs.forEach { (code, label) ->
                                ChipButton(label = label, selected = state.lang == code, onClick = { vm.setLang(code) })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.m))
                ControlLabel("PERIOD")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HoroscopePeriod.DAILY to "Day", HoroscopePeriod.WEEKLY to "Week", HoroscopePeriod.MONTHLY to "Month").forEach { (p, l) ->
                        ChipButton(label = l, selected = state.period == p, onClick = { vm.setPeriod(p) })
                    }
                }

                Spacer(Modifier.height(Spacing.m))
                ControlLabel("DATE / KEY")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    NavArrow("←") { vm.navigateDate(false) }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.run {
                                when (period) {
                                    HoroscopePeriod.DAILY   -> selectedDate.toString()
                                    HoroscopePeriod.WEEKLY  -> "${selectedDate.year}-W${((selectedDate.dayOfYear / 7) + 1).toString().padStart(2, '0')}"
                                    HoroscopePeriod.MONTHLY -> "${selectedDate.year}-${selectedDate.monthNumber.toString().padStart(2, '0')}"
                                }
                            },
                            color = AppColors.AccentGold, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                        )
                    }
                    NavArrow("→") { vm.navigateDate(true) }
                }

                Spacer(Modifier.height(Spacing.m))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    ActionButton(text = if (state.isLoading) "Loading…" else "Load", enabled = !state.isLoading && !state.isSaving, onClick = { vm.load() })
                    when {
                        state.isLoaded          -> StatusText("✓ Loaded", Color(0xFF6FCF97))
                        state.loadError != null -> StatusText("✗ ${state.loadError}", Color(0xFFEB5757))
                    }
                }

                Spacer(Modifier.height(Spacing.m))
                ControlLabel("GENERATE ALL SIGNS")
                Spacer(Modifier.height(6.dp))
                val remaining    = state.generatingSignIds.size
                val total        = 12
                val genAllEnabled = !state.isGeneratingAll && !state.isLoading && !state.isSaving
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.s))
                        .background(
                            if (genAllEnabled) Brush.horizontalGradient(listOf(AppColors.AccentGold.copy(alpha = 0.14f), AppColors.AccentGold.copy(alpha = 0.07f)))
                            else Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface))
                        )
                        .border(1.dp, AppColors.AccentGold.copy(alpha = if (genAllEnabled) 0.5f else 0.2f), RoundedCornerShape(Radius.s))
                        .clickable(enabled = genAllEnabled) { vm.generateAllSigns() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.isGeneratingAll) "✦ Generating… ($remaining/$total)" else "✦ Generate all signs",
                        color = if (genAllEnabled) AppColors.AccentGold else AppColors.TextDim,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    )
                }
                AnimatedVisibility(visible = state.isGeneratingAll) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        val progress = (total - remaining).toFloat() / total
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).background(AppColors.Surface)) {
                            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(AppColors.AccentGold.copy(alpha = 0.7f), AppColors.AccentGold))))
                        }
                    }
                }
                state.generateError?.let { err ->
                    Spacer(Modifier.height(6.dp))
                    Text("✗ $err", color = Color(0xFFEB5757), fontSize = 11.sp)
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(top = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                ALL_SIGNS.forEach { sign ->
                    val horoscope    = state.horoscopes[sign.id] ?: HoroscopeResponse(text = "", love = 75, career = 75, health = 75, energy = 75)
                    val isGenerating = state.generatingSignIds.contains(sign.id)
                    SignCard(
                        sign        = sign,
                        horoscope   = horoscope,
                        isGenerating = isGenerating,
                        onTextChange  = { vm.updateText(sign.id, it) },
                        onScoreChange = { field, delta -> vm.updateScore(sign.id, field, delta) },
                        onGenerate    = { vm.generateForSign(sign) },
                    )
                }
            }
            Spacer(Modifier.height(100.dp))
        }

        SaveBar(
            isSaving  = state.isSaving,
            isLoading = state.isLoading,
            savedText = if (state.savedCount >= 0) "✓ Saved: ${state.savedCount} signs" else null,
            errorText = state.saveError?.let { "✗ $it" },
            onSave    = { vm.saveAll() },
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sign card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SignCard(
    sign: ZodiacSign, horoscope: HoroscopeResponse, isGenerating: Boolean,
    onTextChange: (String) -> Unit, onScoreChange: (String, Int) -> Unit, onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m)).background(AppColors.Card)
            .border(1.dp, if (isGenerating) AppColors.AccentGold.copy(alpha = 0.5f) else AppColors.Border, RoundedCornerShape(Radius.m))
            .padding(Spacing.l)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sign.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(sign.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            if (horoscope.text.isNotBlank() && !isGenerating) {
                Text("●", color = Color(0xFF6FCF97), fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(Radius.s))
                    .background(if (isGenerating) AppColors.Surface else AppColors.AccentGold.copy(alpha = 0.12f))
                    .border(1.dp, AppColors.AccentGold.copy(alpha = if (isGenerating) 0.2f else 0.45f), RoundedCornerShape(Radius.s))
                    .clickable(enabled = !isGenerating, onClick = onGenerate)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    if (isGenerating) "✦ Generating…" else "✦ Generate",
                    color = if (isGenerating) AppColors.TextDim else AppColors.AccentGold,
                    fontSize = 10.sp, fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(Spacing.s))
        OutlinedTextField(
            value = horoscope.text, onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
            placeholder = { Text("Enter horoscope for ${sign.name}…", color = AppColors.TextDim, fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AppColors.AccentGold,
                unfocusedBorderColor = AppColors.Border,
                cursorColor          = AppColors.AccentGold,
                focusedContainerColor   = AppColors.CardDark,
                unfocusedContainerColor = AppColors.CardDark,
            ),
            shape = RoundedCornerShape(Radius.s),
        )
        Spacer(Modifier.height(Spacing.s))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreField("❤️", "love",   horoscope.love,   onScoreChange)
                ScoreField("💼", "career", horoscope.career, onScoreChange)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreField("🌿", "health", horoscope.health, onScoreChange)
                ScoreField("⚡", "energy", horoscope.energy, onScoreChange)
            }
        }
    }
}

@Composable
private fun ScoreField(emoji: String, field: String, value: Int, onChange: (String, Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(emoji, fontSize = 12.sp)
        ScoreBtn("−") { onChange(field, -5) }
        Text(
            value.toString(),
            color = when { value >= 85 -> Color(0xFF6FCF97); value >= 65 -> AppColors.AccentGold; else -> Color(0xFFEB5757) },
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.width(26.dp), textAlign = TextAlign.Center,
        )
        ScoreBtn("+") { onChange(field, +5) }
    }
}

@Composable
private fun ScoreBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(AppColors.Surface).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = AppColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

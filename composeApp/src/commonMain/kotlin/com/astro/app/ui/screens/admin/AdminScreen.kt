package com.astro.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.astro.app.data.ALL_SIGNS
import com.astro.app.data.HoroscopeResponse
import com.astro.app.data.HoroscopePeriod
import com.astro.app.data.ZodiacSign
import com.astro.app.ui.theme.*
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AdminScreen(
    vm: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTarot: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val state by vm.state.collectAsState()

    // Автозагрузка при открытии экрана
    LaunchedEffect(Unit) { vm.loadAll() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.s))
                        .clickable { onNavigateBack() }
                        .padding(horizontal = Spacing.m, vertical = Spacing.s)
                ) {
                    Text("← Back", color = AppColors.AccentGold, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Admin Panel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp)) // balance back button
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Border)
            )

            // ── Section tabs ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                AdminTabItem("🌙 Horoscopes", active = true,  onClick = {})
                AdminTabItem("🃏 Tarot",      active = false, onClick = onNavigateToTarot)
                AdminTabItem("🔔 Push",       active = false, onClick = onNavigateToNotifications)
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Controls ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .padding(top = Spacing.l)
            ) {
                // Language
                ControlLabel("LANGUAGE")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ru" to "RU", "uk" to "UK", "en" to "EN").forEach { (code, label) ->
                        ChipButton(
                            label = label,
                            selected = state.lang == code,
                            onClick = { vm.setLang(code) }
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.m))

                // Period
                ControlLabel("PERIOD")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        HoroscopePeriod.DAILY   to "Day",
                        HoroscopePeriod.WEEKLY  to "Week",
                        HoroscopePeriod.MONTHLY to "Month"
                    ).forEach { (period, label) ->
                        ChipButton(
                            label = label,
                            selected = state.period == period,
                            onClick = { vm.setPeriod(period) }
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.m))

                // Date navigator
                ControlLabel("DATE / KEY")
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    NavArrow("←") { vm.navigateDate(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.s))
                            .background(AppColors.Surface)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.run {
                                when (period) {
                                    HoroscopePeriod.DAILY   -> selectedDate.toString()
                                    HoroscopePeriod.WEEKLY  -> {
                                        val week = (selectedDate.dayOfYear / 7) + 1
                                        "${selectedDate.year}-W${week.toString().padStart(2, '0')}"
                                    }
                                    HoroscopePeriod.MONTHLY ->
                                        "${selectedDate.year}-${selectedDate.monthNumber.toString().padStart(2, '0')}"
                                }
                            },
                            color = AppColors.AccentGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    NavArrow("→") { vm.navigateDate(true) }
                }

                Spacer(Modifier.height(Spacing.m))

                // ── Generate All Languages ────────────────────────────────────
                ControlLabel("ALL LANGUAGES (RU + UK + EN)")
                Spacer(Modifier.height(6.dp))
                val genLangsEnabled = !state.genAllLangsLoading && !state.isGeneratingAll &&
                        !state.isLoading && !state.isSaving
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.s))
                        .background(
                            if (genLangsEnabled)
                                Brush.horizontalGradient(listOf(Color(0xFF1A1030), Color(0xFF0D0D18)))
                            else
                                Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface))
                        )
                        .border(
                            1.dp,
                            if (genLangsEnabled) Color(0xFF9B6DFF).copy(alpha = 0.55f) else AppColors.Border,
                            RoundedCornerShape(Radius.s)
                        )
                        .clickable(enabled = genLangsEnabled) { vm.generateAllLanguages() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.genAllLangsLoading)
                            "🌍 Generating… (3 langs × 12 signs)"
                        else
                            "🌍 Generate all languages",
                        color = if (genLangsEnabled) Color(0xFFB89EFF) else AppColors.TextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Result badge
                AnimatedVisibility(
                    visible = state.genAllLangsResult != null,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit  = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                ) {
                    val result = state.genAllLangsResult
                    if (result != null) {
                        val isOk   = result.startsWith("ok:")
                        val isWarn = result.startsWith("⚠")
                        val color  = when {
                            isOk   -> Color(0xFF6FCF97)
                            isWarn -> AppColors.AccentGold
                            else   -> Color(0xFFEB5757)
                        }
                        val display = if (isOk) {
                            val parts = result.removePrefix("ok:").split(" ")
                            val ok   = parts.getOrNull(0) ?: ""
                            val fail = parts.getOrNull(1)
                            if (fail != null) "✓ Generated $ok  $fail" else "✓ Generated $ok / 36"
                        } else result
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.s))
                                .background(color.copy(alpha = 0.08f))
                                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.s))
                                .padding(horizontal = Spacing.m, vertical = Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(display, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { vm.clearGenAllLangsResult() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("✕", color = AppColors.TextDim, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.m))

                // ── Horoscope Prompt editor ───────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    ControlLabel("PROMPT")
                    Spacer(Modifier.weight(1f))
                    if (state.promptLoading) {
                        Text("Loading…", color = AppColors.TextDim, fontSize = 10.sp)
                    }
                    if (state.promptSaved) {
                        Text("✓ Saved", color = Color(0xFF6FCF97), fontSize = 10.sp)
                    }
                    state.promptError?.let {
                        Text("✗ $it", color = Color(0xFFEB5757), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.promptText,
                    onValueChange = { vm.setPromptText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 14,
                    placeholder = {
                        Text(
                            "Style instructions only — e.g. tone, sentence count, what scores mean.\n" +
                            "Signs, date, language and JSON schema are added automatically.",
                            color = AppColors.TextDim, fontSize = 11.sp, lineHeight = 16.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    textStyle = LocalTextStyle.current.copy(
                        color = AppColors.TextSecondary, fontSize = 11.sp, lineHeight = 17.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF9B6DFF).copy(alpha = 0.7f),
                        unfocusedBorderColor = AppColors.Border,
                        cursorColor          = Color(0xFF9B6DFF),
                        focusedContainerColor   = AppColors.CardDark,
                        unfocusedContainerColor = AppColors.CardDark,
                    ),
                    shape = RoundedCornerShape(Radius.s),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    val canSave = !state.promptSaving && !state.promptLoading && state.promptText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.s))
                            .background(if (canSave) Color(0xFF9B6DFF).copy(alpha = 0.18f) else AppColors.Surface)
                            .border(1.dp, Color(0xFF9B6DFF).copy(alpha = if (canSave) 0.5f else 0.2f), RoundedCornerShape(Radius.s))
                            .clickable(enabled = canSave) { vm.savePrompt() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (state.promptSaving) "Saving…" else "Save prompt",
                            color = if (canSave) Color(0xFFB89EFF) else AppColors.TextDim,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.s))
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s))
                            .clickable { vm.loadPrompt() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("↺ Reload", color = AppColors.TextMuted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(Spacing.l))

                // Load button + status
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    ActionButton(
                        text = if (state.isLoading) "Loading…" else "Load",
                        enabled = !state.isLoading && !state.isSaving,
                        onClick = { vm.load() }
                    )
                    when {
                        state.isLoaded  -> StatusText("✓ Loaded", Color(0xFF6FCF97))
                        state.loadError != null -> StatusText("✗ ${state.loadError}", Color(0xFFEB5757))
                    }
                }

                Spacer(Modifier.height(Spacing.m))

                // Generate All block
                ControlLabel("GENERATE ALL SIGNS")
                Spacer(Modifier.height(6.dp))
                val remaining = state.generatingSignIds.size
                val total = 12
                val genAllEnabled = !state.isGeneratingAll && !state.isLoading && !state.isSaving
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.s))
                            .background(
                                if (genAllEnabled)
                                    Brush.horizontalGradient(listOf(AppColors.AccentGold.copy(alpha = 0.14f), AppColors.AccentGold.copy(alpha = 0.07f)))
                                else
                                    Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface))
                            )
                            .border(
                                1.dp,
                                AppColors.AccentGold.copy(alpha = if (genAllEnabled) 0.5f else 0.2f),
                                RoundedCornerShape(Radius.s)
                            )
                            .clickable(enabled = genAllEnabled) { vm.generateAllSigns() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.isGeneratingAll)
                                "✦ Generating… ($remaining/$total)"
                            else
                                "✦ Generate all signs",
                            color = if (genAllEnabled) AppColors.AccentGold else AppColors.TextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Progress bar when generating all
                AnimatedVisibility(visible = state.isGeneratingAll) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        val progress = (total - remaining).toFloat() / total
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(AppColors.Surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(AppColors.AccentGold.copy(alpha = 0.7f), AppColors.AccentGold)
                                        )
                                    )
                            )
                        }
                    }
                }

            }

            // ── Sign cards ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                ALL_SIGNS.forEach { sign ->
                    val horoscope = state.horoscopes[sign.id] ?: HoroscopeResponse(text = "", love = 5, career = 5, health = 5, energy = 5)
                    val isGenerating = state.generatingSignIds.contains(sign.id)
                    SignCard(
                        sign = sign,
                        horoscope = horoscope,
                        isGenerating = isGenerating,
                        onTextChange = { vm.updateText(sign.id, it) },
                        onScoreChange = { field, delta -> vm.updateScore(sign.id, field, delta) },
                        onGenerate = { vm.generateForSign(sign) }
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // ── Floating save bar ─────────────────────────────────────────────────
        SaveBar(
            isSaving  = state.isSaving,
            isLoading = state.isLoading,
            savedText = if (state.savedCount >= 0) "✓ Saved: ${state.savedCount} signs" else null,
            errorText = state.saveError?.let { "✗ $it" },
            onSave    = { vm.saveAll() },
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Sign card ─────────────────────────────────────────────────────────────────

@Composable
private fun SignCard(
    sign: ZodiacSign,
    horoscope: HoroscopeResponse,
    isGenerating: Boolean,
    onTextChange: (String) -> Unit,
    onScoreChange: (String, Int) -> Unit,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .border(
                width = 1.dp,
                color = if (isGenerating) AppColors.AccentGold.copy(alpha = 0.5f) else AppColors.Border,
                shape = RoundedCornerShape(Radius.m)
            )
            .padding(Spacing.l)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = sign.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = sign.name,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            // Filled indicator
            if (horoscope.text.isNotBlank() && !isGenerating) {
                Text("●", color = Color(0xFF6FCF97), fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
            }
            // Generate button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.s))
                    .background(
                        if (isGenerating) AppColors.Surface
                        else AppColors.AccentGold.copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        AppColors.AccentGold.copy(alpha = if (isGenerating) 0.2f else 0.45f),
                        RoundedCornerShape(Radius.s)
                    )
                    .clickable(enabled = !isGenerating, onClick = onGenerate)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isGenerating) "✦ Generating…" else "✦ Generate",
                    color = if (isGenerating) AppColors.TextDim else AppColors.AccentGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(Spacing.s))

        // Text field
        OutlinedTextField(
            value = horoscope.text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = {
                Text(
                    "Enter horoscope for ${sign.name}…",
                    color = AppColors.TextDim,
                    fontSize = 12.sp
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = LocalTextStyle.current.copy(
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = AppColors.AccentGold,
                unfocusedBorderColor = AppColors.Border,
                cursorColor          = AppColors.AccentGold,
                focusedContainerColor   = AppColors.CardDark,
                unfocusedContainerColor = AppColors.CardDark,
            ),
            shape = RoundedCornerShape(Radius.s)
        )

        Spacer(Modifier.height(Spacing.s))

        // Scores — two rows of two
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreField("❤️", "love",   horoscope.love,   onScoreChange)
                ScoreField("💼", "career", horoscope.career, onScoreChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreField("🌿", "health", horoscope.health, onScoreChange)
                ScoreField("⚡", "energy", horoscope.energy, onScoreChange)
            }
        }
    }
}

@Composable
private fun ScoreField(
    emoji: String,
    field: String,
    value: Int,
    onChange: (String, Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(emoji, fontSize = 12.sp)
        ScoreBtn("−") { onChange(field, -5) }
        Text(
            text = value.toString(),
            color = when {
                value >= 85 -> Color(0xFF6FCF97)   // зелёный — хорошо
                value >= 65 -> AppColors.AccentGold // золотой — средне
                else        -> Color(0xFFEB5757)    // красный — плохо
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.Center
        )
        ScoreBtn("+") { onChange(field, +5) }
    }
}

@Composable
private fun ScoreBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = AppColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Shared small components ───────────────────────────────────────────────────

@Composable
private fun ControlLabel(text: String) {
    Text(text, color = AppColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp)
}

@Composable
private fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(if (selected) AppColors.AccentGold else AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF0A0A0F) else AppColors.TextMuted,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun NavArrow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(Radius.s))
            .background(AppColors.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = AppColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.s))
            .background(if (enabled) AppColors.Surface else AppColors.CardDark)
            .border(1.dp, AppColors.AccentGold.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(Radius.s))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (enabled) AppColors.AccentGold else AppColors.TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

// ── Floating save bar (shared between horoscope & tarot admin) ────────────────

@Composable
internal fun SaveBar(
    isSaving: Boolean,
    isLoading: Boolean,
    savedText: String?,
    errorText: String?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, AppColors.Background.copy(alpha = 0.97f)),
                    startY = 0f,
                    endY = 60f
                )
            )
            .padding(horizontal = Spacing.xl)
            .padding(top = Spacing.xl, bottom = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = savedText != null || errorText != null,
            enter = fadeIn(tween(250)) + expandVertically(tween(250)),
            exit  = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    errorText != null -> Text(errorText, color = Color(0xFFEB5757), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    savedText != null -> Text(savedText, color = Color(0xFF6FCF97), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(Spacing.s))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.m))
                .background(if (!isSaving) AppColors.AccentGold else AppColors.Surface)
                .border(1.dp, AppColors.AccentGold.copy(alpha = if (!isSaving) 0f else 0.3f), RoundedCornerShape(Radius.m))
                .clickable(enabled = !isSaving && !isLoading) { onSave() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSaving) "Saving..." else "Save All",
                color = if (!isSaving) Color(0xFF0A0A0F) else AppColors.TextMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

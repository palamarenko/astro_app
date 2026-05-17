package com.iruna.app.ui.screens.admin

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
import com.iruna.app.data.ALL_SIGNS
import com.iruna.app.data.GenerationLogEntry
import com.iruna.app.data.HoroscopeResponse
import com.iruna.app.data.HoroscopePeriod
import com.iruna.app.data.ZodiacSign
import com.iruna.app.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class HoroscopeSubTab { EDIT, FIREFUN }

@Composable
fun AdminScreen(
    vm: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTarot: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val state by vm.state.collectAsState()
    var subTab by remember { mutableStateOf(HoroscopeSubTab.EDIT) }

    LaunchedEffect(Unit) { vm.loadAll() }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).clickable { onNavigateBack() }.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                    Text("← Back", color = AppColors.AccentGold, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("Admin Panel", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // Main tabs
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                AdminTabItem("🌙 Horoscopes", active = true,  onClick = {})
                AdminTabItem("🃏 Tarot",      active = false, onClick = onNavigateToTarot)
                AdminTabItem("🔔 Push",       active = false, onClick = onNavigateToNotifications)
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // Sub-tabs
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubTabButton(label = "✏️ Edit",    active = subTab == HoroscopeSubTab.EDIT,    onClick = { subTab = HoroscopeSubTab.EDIT })
                SubTabButton(label = "🔥 FireFun", active = subTab == HoroscopeSubTab.FIREFUN, onClick = {
                    subTab = HoroscopeSubTab.FIREFUN
                    vm.loadGenerationLogs()
                    vm.loadGenSchedule()
                    vm.loadPrompt()
                })
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            Box(modifier = Modifier.fillMaxSize()) {
                when (subTab) {
                    HoroscopeSubTab.EDIT    -> EditTab(state, vm)
                    HoroscopeSubTab.FIREFUN -> FireFunTab(state, vm)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EDIT TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditTab(state: AdminUiState, vm: AdminViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(top = Spacing.l)) {

                ControlLabel("LANGUAGE")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ru" to "RU", "uk" to "UK", "en" to "EN").forEach { (code, label) ->
                        ChipButton(label = label, selected = state.lang == code, onClick = { vm.setLang(code) })
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
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.run {
                                when (period) {
                                    HoroscopePeriod.DAILY   -> selectedDate.toString()
                                    HoroscopePeriod.WEEKLY  -> "${selectedDate.year}-W${((selectedDate.dayOfYear / 7) + 1).toString().padStart(2, '0')}"
                                    HoroscopePeriod.MONTHLY -> "${selectedDate.year}-${selectedDate.monthNumber.toString().padStart(2, '0')}"
                                }
                            },
                            color = AppColors.AccentGold, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
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
                val remaining = state.generatingSignIds.size
                val total = 12
                val genAllEnabled = !state.isGeneratingAll && !state.isLoading && !state.isSaving
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.s))
                        .background(if (genAllEnabled) Brush.horizontalGradient(listOf(AppColors.AccentGold.copy(alpha = 0.14f), AppColors.AccentGold.copy(alpha = 0.07f))) else Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface)))
                        .border(1.dp, AppColors.AccentGold.copy(alpha = if (genAllEnabled) 0.5f else 0.2f), RoundedCornerShape(Radius.s))
                        .clickable(enabled = genAllEnabled) { vm.generateAllSigns() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (state.isGeneratingAll) "✦ Generating… ($remaining/$total)" else "✦ Generate all signs", color = if (genAllEnabled) AppColors.AccentGold else AppColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(top = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                ALL_SIGNS.forEach { sign ->
                    val horoscope    = state.horoscopes[sign.id] ?: HoroscopeResponse(text = "", love = 5, career = 5, health = 5, energy = 5)
                    val isGenerating = state.generatingSignIds.contains(sign.id)
                    SignCard(sign = sign, horoscope = horoscope, isGenerating = isGenerating, onTextChange = { vm.updateText(sign.id, it) }, onScoreChange = { field, delta -> vm.updateScore(sign.id, field, delta) }, onGenerate = { vm.generateForSign(sign) })
                }
            }
            Spacer(Modifier.height(100.dp))
        }

        SaveBar(isSaving = state.isSaving, isLoading = state.isLoading, savedText = if (state.savedCount >= 0) "✓ Saved: ${state.savedCount} signs" else null, errorText = state.saveError?.let { "✗ $it" }, onSave = { vm.saveAll() }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FIREFUN TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FireFunTab(state: AdminUiState, vm: AdminViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl).padding(top = Spacing.l)
    ) {

        // ── 1. Generate All Languages ─────────────────────────────────────────
        SectionHeader("🌍 Generate All Languages")
        Spacer(Modifier.height(8.dp))

        ControlLabel("PERIOD")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(HoroscopePeriod.DAILY to "Day", HoroscopePeriod.WEEKLY to "Week", HoroscopePeriod.MONTHLY to "Month").forEach { (p, l) ->
                ChipButton(label = l, selected = state.genAllLangsPeriod == p, onClick = { vm.setGenAllLangsPeriod(p) })
            }
        }

        Spacer(Modifier.height(10.dp))
        ControlLabel("DATE / KEY")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            NavArrow("←") { vm.navigateGenAllLangsDate(false) }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s))
                    .background(AppColors.Surface).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.run {
                        when (genAllLangsPeriod) {
                            HoroscopePeriod.DAILY   -> genAllLangsDate.toString()
                            HoroscopePeriod.WEEKLY  -> "${genAllLangsDate.year}-W${((genAllLangsDate.dayOfYear / 7) + 1).toString().padStart(2, '0')}"
                            HoroscopePeriod.MONTHLY -> "${genAllLangsDate.year}-${genAllLangsDate.monthNumber.toString().padStart(2, '0')}"
                        }
                    },
                    color = Color(0xFFB89EFF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                )
            }
            NavArrow("→") { vm.navigateGenAllLangsDate(true) }
        }

        Spacer(Modifier.height(10.dp))
        val genEnabled = !state.genAllLangsLoading && state.functionUrl.isNotBlank() && state.adminSecret.isNotBlank()
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(Radius.s))
                .background(if (genEnabled) Brush.horizontalGradient(listOf(Color(0xFF1A1030), Color(0xFF0D0D18))) else Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface)))
                .border(1.dp, if (genEnabled) Color(0xFF9B6DFF).copy(alpha = 0.55f) else AppColors.Border, RoundedCornerShape(Radius.s))
                .clickable(enabled = genEnabled) { vm.generateAllLanguages() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.genAllLangsLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFFB89EFF), strokeWidth = 2.dp)
                    Text("Generating… RU + UK + EN × 12 signs", color = Color(0xFFB89EFF), fontSize = 12.sp)
                }
            } else {
                Text("🌍 Generate · ${state.genAllLangsPeriod.label}", color = if (genEnabled) Color(0xFFB89EFF) else AppColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        AnimatedVisibility(visible = state.genAllLangsResult != null, enter = fadeIn(tween(250)) + expandVertically(tween(250)), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            val result = state.genAllLangsResult
            if (result != null) {
                val isOk   = result.startsWith("ok:")
                val isWarn = result.startsWith("⚠")
                val color  = when { isOk -> Color(0xFF6FCF97); isWarn -> AppColors.AccentGold; else -> Color(0xFFEB5757) }
                val display = if (isOk) {
                    val parts = result.removePrefix("ok:").split(" ")
                    if (parts.size > 1) "✓ Generated ${parts[0]}  ${parts[1]}" else "✓ Generated ${parts[0]} / 36"
                } else result
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.s)).background(color.copy(alpha = 0.08f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.s)).padding(horizontal = Spacing.m, vertical = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(display, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { vm.clearGenAllLangsResult() }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("✕", color = AppColors.TextDim, fontSize = 11.sp)
                    }
                }
            }
        }

        if (state.functionUrl.isBlank() || state.adminSecret.isBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("⚠ Set Function URL and Admin Secret in ⚙️ Settings", color = AppColors.AccentGold.copy(alpha = 0.7f), fontSize = 11.sp)
        }

        FireFunDivider()

        // ── 2. Prompt editor ─────────────────────────────────────────────────
        SectionHeader("✏️ Generation Prompt")
        Spacer(Modifier.height(8.dp))

        ControlLabel("PERIOD")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(HoroscopePeriod.DAILY to "Day", HoroscopePeriod.WEEKLY to "Week", HoroscopePeriod.MONTHLY to "Month").forEach { (p, l) ->
                ChipButton(label = l, selected = state.period == p, onClick = { vm.setPeriod(p); vm.loadPrompt() })
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            ControlLabel("PROMPT")
            Spacer(Modifier.weight(1f))
            if (state.promptLoading) Text("Loading…", color = AppColors.TextDim, fontSize = 10.sp)
            if (state.promptSaved)   Text("✓ Saved", color = Color(0xFF6FCF97), fontSize = 10.sp)
            state.promptError?.let { Text("✗ $it", color = Color(0xFFEB5757), fontSize = 10.sp) }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = state.promptText, onValueChange = { vm.setPromptText(it) },
            modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 14,
            placeholder = { Text("Style instructions only — tone, sentence count, what scores mean.\nSigns, date, language and JSON schema are added automatically.", color = AppColors.TextDim, fontSize = 11.sp, lineHeight = 16.sp) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 11.sp, lineHeight = 17.sp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF9B6DFF).copy(alpha = 0.7f), unfocusedBorderColor = AppColors.Border, cursorColor = Color(0xFF9B6DFF), focusedContainerColor = AppColors.CardDark, unfocusedContainerColor = AppColors.CardDark),
            shape = RoundedCornerShape(Radius.s),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            val canSave = !state.promptSaving && !state.promptLoading && state.promptText.isNotBlank()
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(if (canSave) Color(0xFF9B6DFF).copy(alpha = 0.18f) else AppColors.Surface).border(1.dp, Color(0xFF9B6DFF).copy(alpha = if (canSave) 0.5f else 0.2f), RoundedCornerShape(Radius.s)).clickable(enabled = canSave) { vm.savePrompt() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(if (state.promptSaving) "Saving…" else "Save prompt", color = if (canSave) Color(0xFFB89EFF) else AppColors.TextDim, fontSize = 12.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s)).clickable { vm.loadPrompt() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("↺ Reload", color = AppColors.TextMuted, fontSize = 12.sp)
            }
        }

        FireFunDivider()

        // ── 3. Generation schedule ────────────────────────────────────────────
        SectionHeader("🕐 Auto-Generation Schedule")
        Spacer(Modifier.height(4.dp))
        Text("Hours when horoscopes are generated automatically (local time)", color = AppColors.TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..23).toList().chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { hour ->
                        val selected = hour in state.genScheduleHours
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(0xFF9B6DFF).copy(alpha = 0.25f) else AppColors.Surface)
                                .border(1.dp, if (selected) Color(0xFF9B6DFF).copy(alpha = 0.7f) else AppColors.Border, RoundedCornerShape(6.dp))
                                .clickable { vm.toggleGenScheduleHour(hour) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(hour.toString().padStart(2, '0'), fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color(0xFFB89EFF) else AppColors.TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val canSave = !state.genScheduleSaving && !state.genScheduleLoading
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(if (canSave) Color(0xFF9B6DFF).copy(alpha = 0.18f) else AppColors.Surface).border(1.dp, Color(0xFF9B6DFF).copy(alpha = if (canSave) 0.5f else 0.2f), RoundedCornerShape(Radius.s)).clickable(enabled = canSave) { vm.saveGenSchedule() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(if (state.genScheduleSaving) "Saving…" else "Save schedule", color = if (canSave) Color(0xFFB89EFF) else AppColors.TextDim, fontSize = 12.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s)).clickable { vm.loadGenSchedule() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("↺ Reload", color = AppColors.TextMuted, fontSize = 12.sp)
            }
            if (state.genScheduleSaved) Text("✓ Saved", color = Color(0xFF6FCF97), fontSize = 11.sp)
            state.genScheduleError?.let { Text("✗ $it", color = Color(0xFFEB5757), fontSize = 11.sp) }
        }
        if (state.genScheduleHours.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Active: ${state.genScheduleHours.sorted().joinToString(", ") { "${it.toString().padStart(2,'0')}:00" }}", color = Color(0xFF9B6DFF).copy(alpha = 0.8f), fontSize = 11.sp)
        }

        FireFunDivider()

        // ── 4. Generation log ─────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("📋 Generation Log")
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s)).clickable { vm.loadGenerationLogs() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("↺ Refresh", color = AppColors.TextMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.logsLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF9B6DFF), strokeWidth = 2.dp)
            }
        } else if (state.generationLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s)).padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text("No generation runs yet", color = AppColors.TextDim, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.generationLogs.forEach { entry -> LogEntryCard(entry) }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Log entry card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LogEntryCard(entry: GenerationLogEntry) {
    val isSuccess = entry.failed == 0
    val color     = if (isSuccess) Color(0xFF6FCF97) else if (entry.success > 0) AppColors.AccentGold else Color(0xFFEB5757)
    val icon      = if (isSuccess) "✓" else if (entry.success > 0) "⚠" else "✗"

    val dt = remember(entry.timestamp) {
        try {
            val instant = Instant.fromEpochMilliseconds(entry.timestamp)
            val local   = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "%02d.%02d.%d %02d:%02d".format(local.dayOfMonth, local.monthNumber, local.year, local.hour, local.minute)
        } catch (e: Exception) { entry.id }
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Radius.s))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(Radius.s))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dt, color = AppColors.TextSecondary, fontSize = 11.sp)
                PeriodBadge(entry.period)
                if (entry.triggeredBy == "scheduled") {
                    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF9B6DFF).copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text("auto", color = Color(0xFFB89EFF), fontSize = 9.sp)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("✓ ${entry.success}/${entry.success + entry.failed}  ·  ${entry.durationMs / 1000}s", color = AppColors.TextMuted, fontSize = 11.sp)
                Text(entry.dateKey, color = AppColors.TextDim, fontSize = 10.sp)
            }
        }
        if (entry.failed > 0) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFEB5757).copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                Text("✗ ${entry.failed}", color = Color(0xFFEB5757), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PeriodBadge(period: String) {
    val (label, color) = when (period) { "weekly" -> "Week" to Color(0xFF56CCF2); "monthly" -> "Month" to Color(0xFFBB6BD9); else -> "Day" to AppColors.AccentGold }
    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable private fun SectionHeader(text: String) { Text(text, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

@Composable
private fun FireFunDivider() {
    Spacer(Modifier.height(Spacing.l))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
    Spacer(Modifier.height(Spacing.l))
}

@Composable
private fun SubTabButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(Radius.s))
            .background(if (active) Color(0xFF9B6DFF).copy(alpha = 0.18f) else AppColors.Surface)
            .border(1.dp, if (active) Color(0xFF9B6DFF).copy(alpha = 0.6f) else AppColors.Border, RoundedCornerShape(Radius.s))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) Color(0xFFB89EFF) else AppColors.TextMuted, fontWeight = if (active) FontWeight.Medium else FontWeight.Normal, fontSize = 12.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sign card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SignCard(sign: ZodiacSign, horoscope: HoroscopeResponse, isGenerating: Boolean, onTextChange: (String) -> Unit, onScoreChange: (String, Int) -> Unit, onGenerate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m)).background(AppColors.Card)
            .border(1.dp, if (isGenerating) AppColors.AccentGold.copy(alpha = 0.5f) else AppColors.Border, RoundedCornerShape(Radius.m)).padding(Spacing.l)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sign.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(sign.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            if (horoscope.text.isNotBlank() && !isGenerating) { Text("●", color = Color(0xFF6FCF97), fontSize = 10.sp); Spacer(Modifier.width(6.dp)) }
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(if (isGenerating) AppColors.Surface else AppColors.AccentGold.copy(alpha = 0.12f)).border(1.dp, AppColors.AccentGold.copy(alpha = if (isGenerating) 0.2f else 0.45f), RoundedCornerShape(Radius.s)).clickable(enabled = !isGenerating, onClick = onGenerate).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(if (isGenerating) "✦ Generating…" else "✦ Generate", color = if (isGenerating) AppColors.TextDim else AppColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(Spacing.s))
        OutlinedTextField(
            value = horoscope.text, onValueChange = onTextChange, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
            placeholder = { Text("Enter horoscope for ${sign.name}…", color = AppColors.TextDim, fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.AccentGold, unfocusedBorderColor = AppColors.Border, cursorColor = AppColors.AccentGold, focusedContainerColor = AppColors.CardDark, unfocusedContainerColor = AppColors.CardDark),
            shape = RoundedCornerShape(Radius.s)
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
        Text(value.toString(), color = when { value >= 85 -> Color(0xFF6FCF97); value >= 65 -> AppColors.AccentGold; else -> Color(0xFFEB5757) }, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        ScoreBtn("+") { onChange(field, +5) }
    }
}

@Composable
private fun ScoreBtn(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).background(AppColors.Surface).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = AppColors.AccentGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared components
// ─────────────────────────────────────────────────────────────────────────────

@Composable private fun ControlLabel(text: String) { Text(text, color = AppColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp) }

@Composable
private fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(Radius.full)).background(if (selected) AppColors.AccentGold else AppColors.Surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color(0xFF0A0A0F) else AppColors.TextMuted, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, fontSize = 12.sp)
    }
}

@Composable
private fun NavArrow(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = AppColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(if (enabled) AppColors.Surface else AppColors.CardDark).border(1.dp, AppColors.AccentGold.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(Radius.s)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 9.dp)) {
        Text(text, color = if (enabled) AppColors.AccentGold else AppColors.TextDim, fontSize = 13.sp)
    }
}

@Composable private fun StatusText(text: String, color: Color) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium) }


@Composable
internal fun SaveBar(isSaving: Boolean, isLoading: Boolean, savedText: String?, errorText: String?, onSave: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, AppColors.Background.copy(alpha = 0.97f)), startY = 0f, endY = 60f))
            .padding(horizontal = Spacing.xl).padding(top = Spacing.xl, bottom = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = savedText != null || errorText != null, enter = fadeIn(tween(250)) + expandVertically(tween(250)), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    errorText != null -> Text(errorText, color = Color(0xFFEB5757), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    savedText != null -> Text(savedText, color = Color(0xFF6FCF97), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(Spacing.s))
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.m))
                .background(if (!isSaving) AppColors.AccentGold else AppColors.Surface)
                .border(1.dp, AppColors.AccentGold.copy(alpha = if (!isSaving) 0f else 0.3f), RoundedCornerShape(Radius.m))
                .clickable(enabled = !isSaving && !isLoading) { onSave() }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isSaving) "Saving..." else "Save All", color = if (!isSaving) Color(0xFF0A0A0F) else AppColors.TextMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

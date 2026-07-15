package com.iruna.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.data.*
import com.iruna.app.i18n.format
import com.iruna.app.ui.theme.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ─────────────────────────────────────────────────────────────────────────────
// FIREFUN TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun FireFunTab(state: AdminUiState, vm: AdminViewModel) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlLabel("LANGUAGES")
            Spacer(Modifier.weight(1f))
            Text(
                if (state.genAllLangsSelected.size == ALL_GEN_LANGS.size) "Deselect all" else "Select all",
                color = Color(0xFFB89EFF), fontSize = 10.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                    ALL_GEN_LANGS.forEach { code ->
                        val all = state.genAllLangsSelected.size == ALL_GEN_LANGS.size
                        if (all == (code in state.genAllLangsSelected)) vm.toggleGenAllLang(code)
                    }
                }.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ALL_GEN_LANG_LABELS.chunked(3).forEach { rowLangs ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLangs.forEach { (code, label) ->
                        ChipButton(label = label, selected = code in state.genAllLangsSelected, onClick = { vm.toggleGenAllLang(code) })
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ControlLabel("DATE / KEY")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            NavArrow("←") { vm.navigateGenAllLangsDate(false) }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).padding(vertical = 10.dp),
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
                    color = Color(0xFFB89EFF), fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                )
            }
            NavArrow("→") { vm.navigateGenAllLangsDate(true) }
        }

        Spacer(Modifier.height(10.dp))
        val genEnabled = !state.genAllLangsLoading && state.functionUrl.isNotBlank() && state.adminSecret.isNotBlank() && state.genAllLangsSelected.isNotEmpty()
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(Radius.s))
                .background(
                    if (genEnabled) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF1A1030), Color(0xFF0D0D18)))
                    else            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(AppColors.Surface, AppColors.Surface))
                )
                .border(1.dp, if (genEnabled) Color(0xFF9B6DFF).copy(alpha = 0.55f) else AppColors.Border, RoundedCornerShape(Radius.s))
                .clickable(enabled = genEnabled) { vm.generateAllLanguages() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.genAllLangsLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFFB89EFF), strokeWidth = 2.dp)
                    val langLabel = state.genAllLangsCurrentLang?.uppercase() ?: "…"
                    Text("Generating $langLabel  ·  ${state.genAllLangsDone} / ${state.genAllLangsSelected.size}", color = Color(0xFFB89EFF), fontSize = 12.sp)
                }
            } else {
                Text("🌍 Generate · ${state.genAllLangsPeriod.label}", color = if (genEnabled) Color(0xFFB89EFF) else AppColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        AnimatedVisibility(visible = state.genAllLangsResult != null, enter = fadeIn(tween(250)) + expandVertically(tween(250)), exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            val result = state.genAllLangsResult
            if (result != null) {
                val isOk    = result.startsWith("ok:")
                val isWarn  = result.startsWith("⚠")
                val color   = when { isOk -> Color(0xFF6FCF97); isWarn -> AppColors.AccentGold; else -> Color(0xFFEB5757) }
                val display = if (isOk) {
                    val parts = result.removePrefix("ok:").split(" ")
                    if (parts.size > 1) "✓ Generated ${parts[0]}  ${parts[1]}" else "✓ Generated ${parts[0]} / ${state.genAllLangsSelected.size * 12}"
                } else result
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.s)).background(color.copy(alpha = 0.08f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.s)).padding(horizontal = Spacing.m, vertical = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
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
            if (state.promptSaved)   Text("✓ Saved",  color = Color(0xFF6FCF97), fontSize = 10.sp)
            state.promptError?.let { Text("✗ $it", color = Color(0xFFEB5757), fontSize = 10.sp) }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = state.promptText, onValueChange = { vm.setPromptText(it) },
            modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 14,
            placeholder = { Text("Style instructions only — tone, sentence count, what scores mean.\nSigns, date, language and JSON schema are added automatically.", color = AppColors.TextDim, fontSize = 11.sp, lineHeight = 16.sp) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 11.sp, lineHeight = 17.sp),
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
                            contentAlignment = Alignment.Center,
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
            Text("Active: ${state.genScheduleHours.sorted().joinToString(", ") { "${it.toString().padStart(2, '0')}:00" }}", color = Color(0xFF9B6DFF).copy(alpha = 0.8f), fontSize = 11.sp)
        }

        FireFunDivider()

        // ── 3b. Auto-generation languages ─────────────────────────────────────
        SectionHeader("🌐 Auto-Generation Languages")
        Spacer(Modifier.height(4.dp))
        Text("Languages generated automatically by the schedule above", color = AppColors.TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlLabel("LANGUAGES")
            Spacer(Modifier.weight(1f))
            Text(
                if (state.genLangsEnabled.size == ALL_GEN_LANGS.size) "Deselect all" else "Select all",
                color = Color(0xFFB89EFF), fontSize = 10.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                    ALL_GEN_LANGS.forEach { code ->
                        val all = state.genLangsEnabled.size == ALL_GEN_LANGS.size
                        if (all == (code in state.genLangsEnabled)) vm.toggleGenLang(code)
                    }
                }.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ALL_GEN_LANG_LABELS.chunked(3).forEach { rowLangs ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLangs.forEach { (code, label) ->
                        ChipButton(label = label, selected = code in state.genLangsEnabled, onClick = { vm.toggleGenLang(code) })
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val canSave = !state.genLangsSaving && !state.genLangsLoading && state.genLangsEnabled.isNotEmpty()
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(if (canSave) Color(0xFF9B6DFF).copy(alpha = 0.18f) else AppColors.Surface).border(1.dp, Color(0xFF9B6DFF).copy(alpha = if (canSave) 0.5f else 0.2f), RoundedCornerShape(Radius.s)).clickable(enabled = canSave) { vm.saveGenLangs() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(if (state.genLangsSaving) "Saving…" else "Save languages", color = if (canSave) Color(0xFFB89EFF) else AppColors.TextDim, fontSize = 12.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(Radius.s)).background(AppColors.Surface).border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s)).clickable { vm.loadGenLangs() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("↺ Reload", color = AppColors.TextMuted, fontSize = 12.sp)
            }
            if (state.genLangsSaved) Text("✓ Saved", color = Color(0xFF6FCF97), fontSize = 11.sp)
            state.genLangsError?.let { Text("✗ $it", color = Color(0xFFEB5757), fontSize = 11.sp) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Auto-generating: ${ALL_GEN_LANGS.filter { it in state.genLangsEnabled }.joinToString(", ") { it.uppercase() }}",
            color = Color(0xFF9B6DFF).copy(alpha = 0.8f), fontSize = 11.sp,
        )

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
            val d = local.dayOfMonth.toString().padStart(2, '0')
            val mo = local.monthNumber.toString().padStart(2, '0')
            val h = local.hour.toString().padStart(2, '0')
            val mi = local.minute.toString().padStart(2, '0')
            "$d.$mo.${local.year} $h:$mi"
        } catch (e: Exception) { entry.id }
    }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.s)).background(color.copy(alpha = 0.06f)).border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(Radius.s)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
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
    val (label, color) = when (period) {
        "weekly"  -> "Week"  to Color(0xFF56CCF2)
        "monthly" -> "Month" to Color(0xFFBB6BD9)
        else      -> "Day"   to AppColors.AccentGold
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(text, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
internal fun FireFunDivider() {
    Spacer(Modifier.height(Spacing.l))
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
    Spacer(Modifier.height(Spacing.l))
}

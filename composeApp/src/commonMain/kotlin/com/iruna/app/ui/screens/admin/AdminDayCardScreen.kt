@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.iruna.app.ui.screens.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
import com.iruna.app.data.ALL_TAROT
import com.iruna.app.data.DayCardContent
import com.iruna.app.data.TarotCard
import com.iruna.app.ui.theme.*

// Full standalone screen — shown when «Карта дня» tab is active in Admin Panel
@Composable
fun AdminDayCardScreen(
    vm: AdminDayCardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHoroscopes: () -> Unit = {},
    onNavigateToTarot: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToBilling: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    // Автозагрузка при открытии экрана
    LaunchedEffect(Unit) { vm.load() }

    val state by vm.state.collectAsState()

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
                    Text("← Назад", color = AppColors.AccentGold, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Admin Panel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
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
                AdminTabItem("🌙 Horoscopes", active = false, onClick = onNavigateToHoroscopes)
                AdminTabItem("🃏 Tarot",      active = false, onClick = onNavigateToTarot)
                AdminTabItem("☀️ Карта дня",  active = true,  onClick = {})
                AdminTabItem("🔔 Push",       active = false, onClick = onNavigateToNotifications)
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
                AdminTabItem("💳 Billing",    active = false, onClick = onNavigateToBilling)
                AdminTabItem("ℹ️ About",      active = false, onClick = onNavigateToAbout)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Content ───────────────────────────────────────────────────────
            DayCardAdminContent(vm = vm)
        }

        // ── Floating save bar ─────────────────────────────────────────────
        SaveBar(
            isSaving  = state.isSaving,
            isLoading = state.isLoading,
            savedText = if (state.savedCount >= 0) "✓ Сохранено карт: ${state.savedCount}" else null,
            errorText = state.saveError?.let { "✗ $it" },
            onSave    = { vm.saveAll() },
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DayCardAdminContent(vm: AdminDayCardViewModel) {
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Hint ──────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.l)) {
            Text(
                "Прогноз «Карты дня» под каждую таро-карту. В приложении карта выбирается по дате — одна на день для всех.",
                color = AppColors.TextMuted, fontSize = 11.sp, lineHeight = 16.sp
            )

            Spacer(Modifier.height(Spacing.l))

            // ── Language ──────────────────────────────────────────────────────
            Text("Язык", color = AppColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ALL_GEN_LANG_LABELS.chunked(4).forEach { rowLangs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowLangs.forEach { (code, label) ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(Radius.full))
                                    .background(if (state.lang == code) AppColors.AccentGold else AppColors.Surface)
                                    .clickable { vm.setLang(code) }
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (state.lang == code) Color(0xFF0A0A0F) else AppColors.TextMuted,
                                    fontWeight = if (state.lang == code) FontWeight.Medium else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.l))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(Radius.s))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.AccentGold.copy(alpha = if (!state.isLoading) 0.5f else 0.2f), RoundedCornerShape(Radius.s))
                        .clickable(enabled = !state.isLoading && !state.isSaving) { vm.load() }
                        .padding(horizontal = 20.dp, vertical = 9.dp)
                ) {
                    Text(
                        if (state.isLoading) "Loading…" else "Load",
                        color = if (!state.isLoading) AppColors.AccentGold else AppColors.TextDim,
                        fontSize = 13.sp
                    )
                }
                when {
                    state.isLoaded -> Text("✓ Loaded", color = Color(0xFF6FCF97), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    state.loadError != null -> Text("✗ ${state.loadError}", color = Color(0xFFEB5757), fontSize = 12.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
        Spacer(Modifier.height(Spacing.m))

        // ── Card list ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            ALL_TAROT.forEach { card ->
                val content = state.cards[card.resourceKey] ?: DayCardContent()
                val isGenerating = state.generatingCardKeys.contains(card.resourceKey)
                DayCardAdminRow(
                    card = card,
                    content = content,
                    isGenerating = isGenerating,
                    onTextChange = { vm.updateText(card.resourceKey, it) },
                    onGenerate = { vm.generateForCard(card) }
                )
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ── Card row ──────────────────────────────────────────────────────────────────

@Composable
private fun DayCardAdminRow(
    card: TarotCard,
    content: DayCardContent,
    isGenerating: Boolean = false,
    onTextChange: (String) -> Unit,
    onGenerate: () -> Unit = {},
) {
    val isFilled = content.text.isNotBlank()

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .border(
                width = 1.dp,
                color = if (isGenerating) AppColors.AccentGold.copy(alpha = 0.5f)
                        else if (isFilled) AppColors.AccentGold.copy(alpha = 0.25f)
                        else AppColors.Border,
                shape = RoundedCornerShape(Radius.m)
            )
            .padding(Spacing.l)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Box(modifier = Modifier.width(52.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(Radius.s))) {
                Image(
                    painter = tarotPainter(card.resourceKey),
                    contentDescription = card.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.number, color = AppColors.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    Text(card.name, color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    if (isFilled && !isGenerating) Text("●", color = Color(0xFF6FCF97), fontSize = 10.sp)
                }
                Text(card.keywords, color = AppColors.TextDim, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
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
                        text = if (isGenerating) "✦ Генерация…" else "✦ Сгенерировать прогноз",
                        color = if (isGenerating) AppColors.TextDim else AppColors.AccentGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.m))

        Text("Прогноз на день", color = AppColors.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(3.dp))
        OutlinedTextField(
            value = content.text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text("Послание/прогноз на сегодня по этой карте…", color = AppColors.TextDim, fontSize = 11.sp) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = AppColors.AccentGold,
                unfocusedBorderColor    = AppColors.Border,
                cursorColor             = AppColors.AccentGold,
                focusedContainerColor   = AppColors.CardDark,
                unfocusedContainerColor = AppColors.CardDark,
            ),
            shape = RoundedCornerShape(Radius.s)
        )
    }
}

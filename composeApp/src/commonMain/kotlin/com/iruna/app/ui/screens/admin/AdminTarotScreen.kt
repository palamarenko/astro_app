@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.iruna.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import com.iruna.app.data.TarotCard
import com.iruna.app.data.TarotCardContent
import com.iruna.app.ui.theme.*
import iruna.composeapp.generated.resources.*
import iruna.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource

// Full standalone screen — shown when Tarot tab is active in Admin Panel
@Composable
fun AdminTarotScreen(
    vm: AdminTarotViewModel,
    adminVm: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHoroscopes: () -> Unit = {},
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
                AdminTabItem("🃏 Tarot",      active = true,  onClick = {})
                AdminTabItem("🔔 Push",       active = false, onClick = onNavigateToNotifications)
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
                AdminTabItem("💳 Billing",    active = false, onClick = onNavigateToBilling)
                AdminTabItem("ℹ️ About",      active = false, onClick = onNavigateToAbout)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Content ───────────────────────────────────────────────────────
            TarotAdminContent(vm = vm)
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

// Called from AdminScreen when Tarot tab is active
@Composable
fun TarotAdminContent(vm: AdminTarotViewModel) {
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Controls ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.l)) {
            Text("Язык", color = AppColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ru" to "RU", "uk" to "UK").forEach { (code, label) ->
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
                val cardContent = state.cards[card.resourceKey] ?: TarotCardContent()
                val isGenerating = state.generatingCardKeys.contains(card.resourceKey)
                TarotAdminCard(
                    card = card,
                    content = cardContent,
                    isGenerating = isGenerating,
                    onFieldChange = { field, text -> vm.updateField(card.resourceKey, field, text) },
                    onGenerate = { vm.generateForCard(card) }
                )
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ── Card row ──────────────────────────────────────────────────────────────────

@Composable
internal fun TarotAdminCard(
    card: TarotCard,
    content: TarotCardContent,
    isGenerating: Boolean = false,
    onFieldChange: (String, String) -> Unit,
    onGenerate: () -> Unit = {},
) {
    val isFilled = content.past.isNotBlank() || content.present.isNotBlank() || content.future.isNotBlank()

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
                        text = if (isGenerating) "✦ Генерация…" else "✦ Сгенерировать",
                        color = if (isGenerating) AppColors.TextDim else AppColors.AccentGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.m))

        listOf(
            Triple("past",    "Прошлое",   content.past),
            Triple("present", "Настоящее", content.present),
            Triple("future",  "Будущее",   content.future),
        ).forEach { (field, label, value) ->
            Text(label, color = AppColors.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
            Spacer(Modifier.height(3.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { onFieldChange(field, it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                placeholder = { Text("Интерпретация — $label…", color = AppColors.TextDim, fontSize = 11.sp) },
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
            Spacer(Modifier.height(Spacing.s))
        }
    }
}

// ── Painter helper ────────────────────────────────────────────────────────────

@Composable
internal fun tarotPainter(resourceKey: String) = when (resourceKey) {
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

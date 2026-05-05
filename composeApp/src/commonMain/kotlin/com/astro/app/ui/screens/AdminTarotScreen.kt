@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.astro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.astro.app.data.ALL_TAROT
import com.astro.app.data.TarotCard
import com.astro.app.data.TarotCardContent
import com.astro.app.ui.theme.*
import com.astro.app.viewmodel.AdminTarotViewModel
import astroapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

// Full standalone screen — shown when Tarot tab is active in Admin Panel
@Composable
fun AdminTarotScreen(
    vm: AdminTarotViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHoroscopes: () -> Unit = {},
) {
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
                    .padding(horizontal = Spacing.xl, vertical = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                // Navigate to Horoscopes
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.s))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.AccentGold.copy(alpha = 0.3f), RoundedCornerShape(Radius.s))
                        .clickable { onNavigateToHoroscopes() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("🌙 Гороскопы", color = AppColors.AccentGold, fontSize = 12.sp)
                }
                // Active tab — Tarot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.s))
                        .background(AppColors.AccentGold)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("🃏 Таро", color = Color(0xFF0A0A0F), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Content ───────────────────────────────────────────────────────
            TarotAdminContent(vm = vm)
        }
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
                        if (state.isLoading) "Загрузка…" else "Загрузить",
                        color = if (!state.isLoading) AppColors.AccentGold else AppColors.TextDim,
                        fontSize = 13.sp
                    )
                }
                when {
                    state.isLoaded -> Text("✓ Загружено", color = Color(0xFF6FCF97), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

        // ── Save All ──────────────────────────────────────────────────────────
        Spacer(Modifier.height(Spacing.xl))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.m))
                    .background(if (!state.isSaving) AppColors.AccentGold else AppColors.Surface)
                    .clickable(enabled = !state.isSaving && !state.isLoading) { vm.saveAll() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.isSaving) "Сохранение…" else "💾  Сохранить всё",
                    color = if (!state.isSaving) Color(0xFF0A0A0F) else AppColors.TextMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(Spacing.s))
            AnimatedVisibility(visible = state.savedCount >= 0 || state.saveError != null, enter = fadeIn(tween(300))) {
                when {
                    state.saveError != null -> Text("✗ ${state.saveError}", color = Color(0xFFEB5757), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    state.savedCount >= 0   -> Text("✓ Сохранено карт: ${state.savedCount}", color = Color(0xFF6FCF97), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(120.dp))
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

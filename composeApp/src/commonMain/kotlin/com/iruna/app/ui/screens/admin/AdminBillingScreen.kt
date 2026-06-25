package com.iruna.app.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.i18n.format
import com.iruna.app.ui.theme.*

@Composable
fun AdminBillingScreen(
    vm: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHoroscopes: () -> Unit = {},
    onNavigateToTarot: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.billingInfo == null && !state.billingLoading) {
            vm.loadBillingInfo()
        }
    }

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
                Spacer(Modifier.width(72.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

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
                AdminTabItem("🔔 Push",       active = false, onClick = onNavigateToNotifications)
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
                AdminTabItem("💳 Billing",    active = true,  onClick = {})
                AdminTabItem("ℹ️ About",      active = false, onClick = onNavigateToAbout)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .padding(top = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {

                // ── Header row with refresh ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BillingSectionLabel("ANTHROPIC API")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.s))
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.4f), RoundedCornerShape(Radius.s))
                            .clickable(enabled = !state.billingLoading) { vm.loadBillingInfo() }
                            .padding(horizontal = Spacing.m, vertical = 6.dp)
                    ) {
                        if (state.billingLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = AppColors.AccentGold,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Text("↻ Обновить", color = AppColors.AccentGold, fontSize = 12.sp)
                        }
                    }
                }

                // ── Model info ────────────────────────────────────────────────
                BillingInfoCard {
                    BillingRow("Модель", "claude-haiku-4-5-20251001")
                    BillingDivider()
                    BillingRow("Input",  "$0.80 / 1M токенов")
                    BillingDivider()
                    BillingRow("Output", "$4.00 / 1M токенов")
                }

                // ── Usage data ────────────────────────────────────────────────
                when {
                    state.billingLoading && state.billingInfo == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Spacing.m)
                            ) {
                                CircularProgressIndicator(color = AppColors.AccentGold, strokeWidth = 2.dp)
                                Text("Загрузка данных...", color = AppColors.TextDim, fontSize = 12.sp)
                            }
                        }
                    }

                    state.billingError != null -> {
                        // Error state
                        BillingSectionLabel("СТАТУС")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.s))
                                .background(Color(0xFF1A0D0D))
                                .border(1.dp, Color(0xFF4A2A2A), RoundedCornerShape(Radius.s))
                                .padding(Spacing.m),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⚠️ Не удалось получить данные", color = Color(0xFFEB5757), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(state.billingError ?: "", color = Color(0xFFEB5757).copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }

                    state.billingInfo != null -> {
                        val info = state.billingInfo!!

                        BillingSectionLabel("ИСПОЛЬЗОВАНИЕ")
                        BillingInfoCard {
                            BillingRow("Input токены",  formatTokens(info.inputTokens))
                            BillingDivider()
                            BillingRow("Output токены", formatTokens(info.outputTokens))
                            if (info.cacheReadTokens > 0) {
                                BillingDivider()
                                BillingRow("Cache read",  formatTokens(info.cacheReadTokens))
                            }
                            if (info.cacheCreationTokens > 0) {
                                BillingDivider()
                                BillingRow("Cache write", formatTokens(info.cacheCreationTokens))
                            }
                        }

                        if (info.estimatedCostUsd > 0) {
                            BillingSectionLabel("ПРИМЕРНАЯ СТОИМОСТЬ")
                            BillingInfoCard {
                                BillingRow(
                                    "Итого (Input + Output)",
                                    "≈ ${"$"}${"%.4f".format(info.estimatedCostUsd)}"
                                )
                            }
                        }

                    }

                    else -> { /* not yet loaded, button below */ }
                }

                // Кнопка консоли показывается всегда
                ConsoleLinkButton()

            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun ConsoleLinkButton() {
    val uriHandler = LocalUriHandler.current
    BillingSectionLabel("БАЛАНС")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.s))
            .background(Color(0xFF0D100D))
            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.2f), RoundedCornerShape(Radius.s))
            .padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m)
    ) {
        Text(
            "Anthropic не предоставляет API для получения остатка — его можно посмотреть только в консоли.",
            color = AppColors.TextDim,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.s))
                .background(AppColors.AccentGold)
                .clickable { uriHandler.openUri("https://console.anthropic.com/settings/billing") }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "💳 Открыть Anthropic Console",
                color = Color(0xFF0A0A0F),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BillingSectionLabel(text: String) {
    Text(
        text = text,
        color = AppColors.TextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun BillingInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.s))
            .background(AppColors.CardDark)
            .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s))
    ) {
        content()
    }
}

@Composable
private fun BillingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.m, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AppColors.TextDim,     fontSize = 13.sp)
        Text(value, color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BillingDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))
}

private fun formatTokens(count: Long): String = when {
    count >= 1_000_000 -> "${"%.2f".format(count / 1_000_000.0)}M"
    count >= 1_000     -> "${"%.1f".format(count / 1_000.0)}K"
    else               -> count.toString()
}

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.iruna.app.ui.theme.*

@Composable
fun AdminNotificationsScreen(
    vm: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHoroscopes: () -> Unit = {},
    onNavigateToTarot: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.loadSchedule() }

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
                AdminTabItem("🔔 Push",       active = true,  onClick = {})
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
                AdminTabItem("ℹ️ About",      active = false, onClick = onNavigateToAbout)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Push Notifications content ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .padding(top = Spacing.xl, bottom = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                // Section label
                Text(
                    text = "PUSH NOTIFICATIONS",
                    color = AppColors.TextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )

                // Description card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.m))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1A1525), Color(0xFF0D0D18))
                            )
                        )
                        .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.m))
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    Text(
                        text = "Send to all subscribers",
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Деплой один раз — работает всегда. Введи URL функции и секрет. Команды ниже.",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }

                // Deploy hint card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.s))
                        .background(Color(0xFF0D1A0D))
                        .border(1.dp, Color(0xFF2A4A2A), RoundedCornerShape(Radius.s))
                        .padding(Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Деплой (один раз):", color = Color(0xFF6FCF97), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("cd functions && npm install", color = Color(0xFF9BE9A8), fontSize = 11.sp)
                    Text("firebase functions:secrets:set ADMIN_SECRET", color = Color(0xFF9BE9A8), fontSize = 11.sp)
                    Text("firebase deploy --only functions", color = Color(0xFF9BE9A8), fontSize = 11.sp)
                }

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    // Send to self
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.m))
                            .background(AppColors.AccentGold.copy(alpha = 0.12f))
                            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.45f), RoundedCornerShape(Radius.m))
                            .clickable(enabled = !state.pushSending) { vm.sendPushToSelf() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🔔  Send to self (test)",
                            color = AppColors.AccentGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Send to all
                    val canSendAll = !state.pushSending && state.functionUrl.isNotBlank() && state.adminSecret.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.m))
                            .background(
                                if (canSendAll) AppColors.AccentGold
                                else AppColors.Surface
                            )
                            .border(
                                1.dp,
                                AppColors.AccentGold.copy(alpha = if (canSendAll) 0f else 0.25f),
                                RoundedCornerShape(Radius.m)
                            )
                            .clickable(enabled = canSendAll) { vm.sendPushToAll() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (state.pushSending) "Sending…" else "📡  Send to all users",
                            color = if (canSendAll) Color(0xFF0A0A0F) else AppColors.TextDim,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Result badge
                AnimatedVisibility(
                    visible = state.pushResult != null,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit  = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                ) {
                    val result = state.pushResult
                    if (result != null) {
                        val (text, color) = when (result) {
                            "ok_self" -> "✓  Local notification sent" to Color(0xFF6FCF97)
                            "ok_all"  -> "✓  Sent to all subscribers via FCM" to Color(0xFF6FCF97)
                            else      -> "✗  $result" to Color(0xFFEB5757)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.s))
                                .background(color.copy(alpha = 0.08f))
                                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(Radius.s))
                                .padding(horizontal = Spacing.m, vertical = Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { vm.clearPushResult() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("✕", color = AppColors.TextDim, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // ── Schedule ───────────────────────────────────────────────────
                Spacer(Modifier.height(Spacing.l))

                Text(
                    text = "DAILY SCHEDULE",
                    color = AppColors.TextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                )

                // Schedule card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.m))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1A1525), Color(0xFF0D0D18))
                            )
                        )
                        .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.m))
                        .padding(Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Время отправки (локальное)",
                                    color = AppColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1A3A1A))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "any TZ",
                                        color = Color(0xFF6FCF97),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            Text(
                                text = if (state.scheduleHours.isEmpty()) "Не выбрано — рассылка отключена"
                                       else "Выбрано часов: ${state.scheduleHours.size}",
                                color = AppColors.TextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        // Load button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Radius.s))
                                .background(AppColors.Surface)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s))
                                .clickable(enabled = !state.scheduleLoading && state.functionUrl.isNotBlank() && state.adminSecret.isNotBlank()) {
                                    vm.loadSchedule()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.scheduleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = AppColors.AccentGold,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("↓ Load", color = AppColors.AccentGold, fontSize = 12.sp)
                            }
                        }
                    }

                    // 24-hour grid: 4 rows × 6 cols
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0..3) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                for (col in 0..5) {
                                    val hour = row * 6 + col
                                    val active = hour in state.scheduleHours
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (active) AppColors.AccentGold
                                                else AppColors.Surface
                                            )
                                            .border(
                                                1.dp,
                                                if (active) AppColors.AccentGold
                                                else AppColors.Border,
                                                RoundedCornerShape(6.dp),
                                            )
                                            .clickable { vm.toggleScheduleHour(hour) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = hour.toString().padStart(2, '0'),
                                            color = if (active) Color(0xFF0A0A0F) else AppColors.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Save button
                    val canSave = !state.scheduleSaving &&
                                  state.functionUrl.isNotBlank() &&
                                  state.adminSecret.isNotBlank()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.m))
                            .background(
                                if (canSave) AppColors.AccentGold.copy(alpha = 0.15f) else AppColors.Surface
                            )
                            .border(
                                1.dp,
                                if (canSave) AppColors.AccentGold.copy(alpha = 0.5f) else AppColors.Border,
                                RoundedCornerShape(Radius.m),
                            )
                            .clickable(enabled = canSave) { vm.saveSchedule() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.scheduleSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AppColors.AccentGold,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "💾  Save schedule",
                                color = if (canSave) AppColors.AccentGold else AppColors.TextDim,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Schedule status badge
                    AnimatedVisibility(
                        visible = state.scheduleSaved || state.scheduleError != null,
                        enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                        exit  = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                    ) {
                        val (badgeText, badgeColor) = if (state.scheduleSaved) {
                            "✓  Schedule saved" to Color(0xFF6FCF97)
                        } else {
                            "✗  ${state.scheduleError}" to Color(0xFFEB5757)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.s))
                                .background(badgeColor.copy(alpha = 0.08f))
                                .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(Radius.s))
                                .padding(horizontal = Spacing.m, vertical = Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                badgeText,
                                color = badgeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

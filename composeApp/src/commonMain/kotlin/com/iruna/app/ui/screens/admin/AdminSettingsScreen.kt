package com.iruna.app.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.iruna.app.ui.theme.*

@Composable
fun AdminSettingsScreen(
    vm: AdminViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHoroscopes: () -> Unit = {},
    onNavigateToTarot: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
) {
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
                AdminTabItem("⚙️ Settings",   active = true,  onClick = {})
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Settings content ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .padding(top = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                Text(
                    text = "CLOUD FUNCTION",
                    color = AppColors.TextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = state.functionUrl,
                    onValueChange = { vm.setFunctionUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Cloud Function URL", color = AppColors.TextDim, fontSize = 11.sp) },
                    placeholder = { Text("https://adminapi-xxx-uc.a.run.app", color = AppColors.TextDim, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AppColors.AccentGold,
                        unfocusedBorderColor = AppColors.Border,
                        focusedLabelColor    = AppColors.AccentGold,
                        unfocusedLabelColor  = AppColors.TextDim,
                        cursorColor          = AppColors.AccentGold,
                        focusedContainerColor   = AppColors.CardDark,
                        unfocusedContainerColor = AppColors.CardDark,
                    ),
                    shape = RoundedCornerShape(Radius.s),
                )

                OutlinedTextField(
                    value = state.adminSecret,
                    onValueChange = { vm.setAdminSecret(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Admin Secret", color = AppColors.TextDim, fontSize = 11.sp) },
                    placeholder = { Text("firebase functions:secrets:set ADMIN_SECRET", color = AppColors.TextDim, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = LocalTextStyle.current.copy(color = AppColors.TextSecondary, fontSize = 12.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AppColors.AccentGold,
                        unfocusedBorderColor = AppColors.Border,
                        focusedLabelColor    = AppColors.AccentGold,
                        unfocusedLabelColor  = AppColors.TextDim,
                        cursorColor          = AppColors.AccentGold,
                        focusedContainerColor   = AppColors.CardDark,
                        unfocusedContainerColor = AppColors.CardDark,
                    ),
                    shape = RoundedCornerShape(Radius.s),
                )

                // Deploy hint
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
                    Text("firebase functions:secrets:set CLAUDE_API_KEY", color = Color(0xFF9BE9A8), fontSize = 11.sp)
                    Text("firebase deploy --only functions", color = Color(0xFF9BE9A8), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ── Shared tab item ───────────────────────────────────────────────────────────

@Composable
internal fun AdminTabItem(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.s))
            .background(if (active) AppColors.AccentGold else AppColors.Surface)
            .then(
                if (!active) Modifier.border(1.dp, AppColors.AccentGold.copy(alpha = 0.3f), RoundedCornerShape(Radius.s))
                else Modifier
            )
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (active) Color(0xFF0A0A0F) else AppColors.AccentGold,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}

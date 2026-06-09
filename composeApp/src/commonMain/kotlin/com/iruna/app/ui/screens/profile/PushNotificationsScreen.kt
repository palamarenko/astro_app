package com.iruna.app.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import iruna.composeapp.generated.resources.*
import com.iruna.app.data.ALL_SIGNS
import com.iruna.app.data.UserProfile
import com.iruna.app.data.UserStorage
import com.iruna.app.i18n.localizedName
import com.iruna.app.i18n.str
import com.iruna.app.notifications.rememberPushPermissionLauncher
import com.iruna.app.notifications.sendLocalTestPush
import com.iruna.app.notifications.subscribeToPushTopic
import com.iruna.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun PushNotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Читаем текущий статус из хранилища
    var profile by remember { mutableStateOf(UserStorage.load() ?: UserProfile()) }
    val enabled = profile.pushNotificationsEnabled

    val requestPermission = rememberPushPermissionLauncher { granted ->
        val updated = profile.copy(
            pushNotificationsAsked   = true,
            pushNotificationsEnabled = granted,
        )
        UserStorage.save(updated)
        profile = updated
        if (granted) subscribeToPushTopic()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(Spacing.xxl))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.AccentGold.copy(alpha = 0.10f))
                        .border(1.dp, AppColors.AccentGold.copy(alpha = 0.28f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = AppColors.AccentGold, fontSize = 18.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = str.push_prompt_title,
                    fontSize = AppType.caption,
                    color = AppColors.AccentGold,
                    letterSpacing = TextUnit(0.18f, TextUnitType.Em),
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp))
            }

            Spacer(Modifier.height(Spacing.xxl))

            // ── Witch image ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(Res.drawable.iruna),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            // ── Status card ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A1525), Color(0xFF0D0D18))
                        )
                    )
                    .border(
                        1.dp,
                        if (enabled) AppColors.AccentGold.copy(alpha = 0.35f)
                        else AppColors.Border,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                // Status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) Color(0xFF6FCF97) else AppColors.TextDim
                            )
                    )
                    Text(
                        text = if (enabled) str.push_status_enabled else str.push_status_disabled,
                        color = if (enabled) Color(0xFF6FCF97) else AppColors.TextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = str.push_prompt_body,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }

            Spacer(Modifier.height(Spacing.l))

            // ── Main action button ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                if (!enabled) {
                    // Enable button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.AccentGold)
                            .clickable { requestPermission() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = str.push_prompt_allow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                } else {
                    // Test notification button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.AccentGold.copy(alpha = 0.12f))
                            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable {
                                val signName = ALL_SIGNS
                                    .find { it.id == profile.signId }
                                    ?.localizedName() ?: profile.signId
                                sendLocalTestPush(signName)
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🔔  ${str.notif_daily_title}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.AccentGold,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Disable button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
                            .clickable {
                                val updated = profile.copy(
                                    pushNotificationsEnabled = false,
                                )
                                UserStorage.save(updated)
                                profile = updated
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = str.push_prompt_deny,
                            fontSize = 14.sp,
                            color = AppColors.TextDim,
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ── Push Prompt Dialog (shown inline from Horoscope screen) ──────────────────

@Composable
fun PushPromptDialog(
    onAllow: () -> Unit,
    onDeny:  () -> Unit,
) {
    // Не используем Dialog-обёртку: на iOS Compose Multiplatform Dialog создаёт отдельный
    // UIWindow и иногда не закрывается при изменении состояния.
    // Родитель (HoroscopeScreen) уже рендерит этот компонент через if(showPushPrompt),
    // поэтому достаточно обычного Box-overlay поверх основного контента.
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .clickable(onClick = onDeny),  // клик по фону = «Позже»
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF1A1525), Color(0xFF0D0D18)))
                )
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},  // поглощаем клики внутри карточки, чтобы не триггерить onDeny
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Witch image
            Image(
                painter = painterResource(Res.drawable.iruna),
                contentDescription = null,
                modifier = androidx.compose.ui.Modifier.size(100.dp),
                contentScale = ContentScale.Fit,
            )

            // Title
            Text(
                text = str.push_prompt_title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.AccentGold,
                textAlign = TextAlign.Center,
            )

            // Body
            Text(
                text = str.push_prompt_body,
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(androidx.compose.ui.Modifier.height(4.dp))

            // Allow button
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.AccentGold)
                    .clickable { onAllow() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = str.push_prompt_allow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }

            // Deny
            Text(
                text = str.push_prompt_deny,
                fontSize = 13.sp,
                color = AppColors.TextDim,
                modifier = androidx.compose.ui.Modifier
                    .clickable { onDeny() }
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

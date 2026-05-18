package com.iruna.app.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.ui.theme.*

private enum class HoroscopeSubTab { EDIT, FIREFUN, CALENDAR }

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

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().background(AppColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(Radius.s)).clickable { onNavigateBack() }.padding(horizontal = Spacing.m, vertical = Spacing.s)
                ) { Text("← Back", color = AppColors.AccentGold, fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                Text("Admin Panel", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Main tabs ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                AdminTabItem("🌙 Horoscopes", active = true,  onClick = {})
                AdminTabItem("🃏 Tarot",      active = false, onClick = onNavigateToTarot)
                AdminTabItem("🔔 Push",       active = false, onClick = onNavigateToNotifications)
                AdminTabItem("⚙️ Settings",   active = false, onClick = onNavigateToSettings)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Sub-tabs ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubTabButton("✏️ Edit",     active = subTab == HoroscopeSubTab.EDIT,     onClick = { subTab = HoroscopeSubTab.EDIT })
                SubTabButton("🔥 FireFun",  active = subTab == HoroscopeSubTab.FIREFUN,  onClick = {
                    subTab = HoroscopeSubTab.FIREFUN
                    vm.loadGenerationLogs()
                    vm.loadGenSchedule()
                    vm.loadPrompt()
                })
                SubTabButton("📅 Calendar", active = subTab == HoroscopeSubTab.CALENDAR, onClick = {
                    subTab = HoroscopeSubTab.CALENDAR
                    vm.loadCalendarData()
                })
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // ── Tab content ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (subTab) {
                    HoroscopeSubTab.EDIT     -> EditTab(state, vm)
                    HoroscopeSubTab.FIREFUN  -> FireFunTab(state, vm)
                    HoroscopeSubTab.CALENDAR -> CalendarTab(state, vm, onNavigateToEdit = { date ->
                        vm.setDateAbsolute(date, state.calendarPeriod)
                        subTab = HoroscopeSubTab.EDIT
                    })
                }
            }
        }
    }
}

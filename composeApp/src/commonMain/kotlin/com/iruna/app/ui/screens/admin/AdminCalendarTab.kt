package com.iruna.app.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.data.HoroscopePeriod
import com.iruna.app.ui.theme.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

// ── Date helpers ──────────────────────────────────────────────────────────────

private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11            -> 30
    2                      -> if (isLeapYear(year)) 29 else 28
    else                   -> 30
}

private fun monthName(m: Int) =
    listOf("January","February","March","April","May","June","July","August","September","October","November","December")[m - 1]

private fun shortMonthName(m: Int) =
    listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[m - 1]

private fun dayOfYearToDate(year: Int, dayOfYear: Int): LocalDate? {
    var rem = dayOfYear
    for (m in 1..12) {
        val dim = daysInMonth(year, m)
        if (rem <= dim) return try { LocalDate(year, m, rem) } catch (e: Exception) { null }
        rem -= dim
    }
    return null
}

// ── Root composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarTab(
    state: AdminUiState,
    vm: AdminViewModel,
    onNavigateToEdit: (LocalDate) -> Unit,
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = AppColors.CardDark,
            shape            = RoundedCornerShape(Radius.m),
            title = { Text("Delete period?", color = AppColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target, color = Color(0xFFEB5757), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "All horoscopes for this period will be removed across all languages (RU, UK, EN, ES, DE, FR). This cannot be undone.",
                        color = AppColors.TextDim, fontSize = 12.sp, lineHeight = 17.sp,
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.s))
                        .background(Color(0xFFEB5757).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFEB5757).copy(alpha = 0.55f), RoundedCornerShape(Radius.s))
                        .clickable { vm.deletePeriod(target); deleteTarget = null }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) { Text("Delete", color = Color(0xFFEB5757), fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.s))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s))
                        .clickable { deleteTarget = null }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) { Text("Cancel", color = AppColors.TextMuted, fontSize = 13.sp) }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl).padding(top = Spacing.l)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Horoscope Calendar", color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.s))
                    .background(if (state.backfillLoading) AppColors.CardDark else AppColors.Surface)
                    .border(1.dp, if (state.backfillLoading) AppColors.Border.copy(alpha = 0.3f) else Color(0xFF9B6DFF).copy(alpha = 0.45f), RoundedCornerShape(Radius.s))
                    .clickable(enabled = !state.backfillLoading && !state.calendarLoading) { vm.backfillMeta() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (state.backfillLoading) "⟳ Filling…" else "⟳ Backfill",
                    color = if (state.backfillLoading) AppColors.TextDim else Color(0xFFB89EFF),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.s))
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s))
                    .clickable { vm.loadCalendarData() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("↺ Reload", color = AppColors.TextMuted, fontSize = 11.sp) }
        }

        state.backfillResult?.let { result ->
            Spacer(Modifier.height(6.dp))
            Text(result, color = if (result.startsWith("✓")) Color(0xFF6FCF97) else Color(0xFFEB5757), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(Spacing.m))

        // ── Period selector ───────────────────────────────────────────────────
        ControlLabel("PERIOD")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(HoroscopePeriod.DAILY to "Day", HoroscopePeriod.WEEKLY to "Week", HoroscopePeriod.MONTHLY to "Month").forEach { (p, l) ->
                ChipButton(label = l, selected = state.calendarPeriod == p, onClick = { vm.setCalendarPeriod(p) })
            }
        }

        Spacer(Modifier.height(Spacing.l))

        // ── Content ───────────────────────────────────────────────────────────
        if (state.calendarLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), color = AppColors.AccentGold, strokeWidth = 2.dp)
            }
        } else {
            when (state.calendarPeriod) {
                HoroscopePeriod.DAILY   -> DailyCalendarView(state, today, vm, onNavigateToEdit, onLongClick = { deleteTarget = it })
                HoroscopePeriod.WEEKLY  -> WeeklyCalendarView(state, today, vm, onNavigateToEdit, onLongClick = { deleteTarget = it })
                HoroscopePeriod.MONTHLY -> MonthlyCalendarView(state, today, vm, onNavigateToEdit, onLongClick = { deleteTarget = it })
            }
        }

        // ── Legend ────────────────────────────────────────────────────────────
        Spacer(Modifier.height(Spacing.l))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CalLegendItem(Color(0xFF6FCF97).copy(alpha = 0.2f), Color(0xFF6FCF97).copy(alpha = 0.5f), "Full (72/72)")
            CalLegendItem(Color(0xFFF2994A).copy(alpha = 0.2f), Color(0xFFF2994A).copy(alpha = 0.5f), "Partial")
            CalLegendItem(AppColors.Surface, AppColors.Border, "Empty")
            CalLegendItem(AppColors.AccentGold.copy(alpha = 0.15f), AppColors.AccentGold.copy(alpha = 0.7f), "Today")
        }
        Spacer(Modifier.height(6.dp))
        Text("Long press on a cell to delete that period", color = AppColors.TextDim, fontSize = 10.sp)
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun CalLegendItem(bg: Color, border: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(bg).border(1.dp, border, RoundedCornerShape(3.dp)))
        Text(label, color = AppColors.TextDim, fontSize = 10.sp)
    }
}

// ── Daily (month grid) ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyCalendarView(
    state: AdminUiState,
    today: LocalDate,
    vm: AdminViewModel,
    onNavigateToEdit: (LocalDate) -> Unit,
    onLongClick: (String) -> Unit,
) {
    val year  = state.calendarViewYear
    val month = state.calendarViewMonth

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        NavArrow("←") { vm.navigateCalendarMonth(false) }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("${monthName(month)} $year", color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        NavArrow("→") { vm.navigateCalendarMonth(true) }
    }

    Spacer(Modifier.height(6.dp))
    val monthPrefix  = "$year-${month.toString().padStart(2, '0')}-"
    val fullCount    = state.calendarMeta.count { (k, v) -> k.startsWith(monthPrefix) && v >= 72 }
    val partialCount = state.calendarMeta.count { (k, v) -> k.startsWith(monthPrefix) && v in 1..71 }
    val totalDays    = daysInMonth(year, month)
    Text(
        buildString {
            append("$fullCount / $totalDays full")
            if (partialCount > 0) append("  ·  $partialCount partial")
        },
        color = AppColors.TextDim, fontSize = 11.sp,
    )
    Spacer(Modifier.height(Spacing.m))

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Mo","Tu","We","Th","Fr","Sa","Su").forEach { h ->
            Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(h, color = AppColors.TextDim, fontSize = 10.sp)
            }
        }
    }

    val firstDow = LocalDate(year, month, 1).dayOfWeek.ordinal
    val cells    = buildList<Int?> {
        repeat(firstDow) { add(null) }
        for (d in 1..totalDays) add(d)
        while (size % 7 != 0) add(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dateKey   = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                        val count     = state.calendarMeta[dateKey] ?: 0
                        val isFull    = count >= 72
                        val isPartial = count in 1..71
                        val isToday   = today.year == year && today.monthNumber == month && today.dayOfMonth == day
                        Box(
                            modifier = Modifier
                                .weight(1f).aspectRatio(1f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(when { isFull -> Color(0xFF6FCF97).copy(alpha = 0.18f); isPartial -> Color(0xFFF2994A).copy(alpha = 0.18f); isToday -> AppColors.AccentGold.copy(alpha = 0.12f); else -> AppColors.Surface })
                                .border(1.dp, when { isToday -> AppColors.AccentGold.copy(alpha = 0.65f); isFull -> Color(0xFF6FCF97).copy(alpha = 0.45f); isPartial -> Color(0xFFF2994A).copy(alpha = 0.5f); else -> AppColors.Border }, RoundedCornerShape(5.dp))
                                .combinedClickable(onClick = { onNavigateToEdit(LocalDate(year, month, day)) }, onLongClick = { onLongClick(dateKey) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    day.toString(),
                                    color = when { isToday -> AppColors.AccentGold; isFull -> Color(0xFF6FCF97); isPartial -> Color(0xFFF2994A); else -> AppColors.TextMuted },
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday || isFull || isPartial) FontWeight.Medium else FontWeight.Normal,
                                )
                                if (isPartial) Text("$count/72", color = Color(0xFFF2994A).copy(alpha = 0.8f), fontSize = 7.sp, lineHeight = 8.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Weekly (year grid of weeks) ───────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeeklyCalendarView(
    state: AdminUiState,
    today: LocalDate,
    vm: AdminViewModel,
    onNavigateToEdit: (LocalDate) -> Unit,
    onLongClick: (String) -> Unit,
) {
    val year       = state.calendarViewYear
    val daysInYear = if (isLeapYear(year)) 366 else 365
    val maxWeek    = (daysInYear / 7) + 1
    val todayWeek  = if (today.year == year) (today.dayOfYear / 7) + 1 else -1

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        NavArrow("←") { vm.navigateCalendarYear(false) }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(year.toString(), color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        NavArrow("→") { vm.navigateCalendarYear(true) }
    }

    Spacer(Modifier.height(6.dp))
    val fullCount    = state.calendarMeta.count { (k, v) -> k.startsWith("$year-W") && v >= 72 }
    val partialCount = state.calendarMeta.count { (k, v) -> k.startsWith("$year-W") && v in 1..71 }
    Text(buildString { append("$fullCount / $maxWeek full"); if (partialCount > 0) append("  ·  $partialCount partial") }, color = AppColors.TextDim, fontSize = 11.sp)
    Spacer(Modifier.height(Spacing.m))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..maxWeek).toList().chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { week ->
                    val weekKey   = "$year-W${week.toString().padStart(2, '0')}"
                    val count     = state.calendarMeta[weekKey] ?: 0
                    val isFull    = count >= 72
                    val isPartial = count in 1..71
                    val isCurrent = week == todayWeek
                    val firstDay  = dayOfYearToDate(year, (week - 1) * 7 + 1)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(when { isFull -> Color(0xFF6FCF97).copy(alpha = 0.18f); isPartial -> Color(0xFFF2994A).copy(alpha = 0.18f); isCurrent -> AppColors.AccentGold.copy(alpha = 0.12f); else -> AppColors.Surface })
                            .border(1.dp, when { isCurrent -> AppColors.AccentGold.copy(alpha = 0.65f); isFull -> Color(0xFF6FCF97).copy(alpha = 0.45f); isPartial -> Color(0xFFF2994A).copy(alpha = 0.5f); else -> AppColors.Border }, RoundedCornerShape(6.dp))
                            .combinedClickable(onClick = { firstDay?.let(onNavigateToEdit) }, onLongClick = { onLongClick(weekKey) })
                            .heightIn(min = 52.dp)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "W${week.toString().padStart(2, '0')}",
                                color = when { isCurrent -> AppColors.AccentGold; isFull -> Color(0xFF6FCF97); isPartial -> Color(0xFFF2994A); else -> AppColors.TextMuted },
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent || isFull || isPartial) FontWeight.Medium else FontWeight.Normal,
                            )
                            if (isPartial) Text("$count/72", color = Color(0xFFF2994A).copy(alpha = 0.8f), fontSize = 8.sp, lineHeight = 9.sp)
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

// ── Monthly (year grid of months) ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthlyCalendarView(
    state: AdminUiState,
    today: LocalDate,
    vm: AdminViewModel,
    onNavigateToEdit: (LocalDate) -> Unit,
    onLongClick: (String) -> Unit,
) {
    val year = state.calendarViewYear

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        NavArrow("←") { vm.navigateCalendarYear(false) }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(year.toString(), color = AppColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        NavArrow("→") { vm.navigateCalendarYear(true) }
    }

    Spacer(Modifier.height(6.dp))
    val fullCount    = state.calendarMeta.count { (k, v) -> k.startsWith("$year-") && v >= 72 }
    val partialCount = state.calendarMeta.count { (k, v) -> k.startsWith("$year-") && v in 1..71 }
    Text(buildString { append("$fullCount / 12 full"); if (partialCount > 0) append("  ·  $partialCount partial") }, color = AppColors.TextDim, fontSize = 11.sp)
    Spacer(Modifier.height(Spacing.m))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..12).toList().chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { month ->
                    val monthKey  = "$year-${month.toString().padStart(2, '0')}"
                    val count     = state.calendarMeta[monthKey] ?: 0
                    val isFull    = count >= 72
                    val isPartial = count in 1..71
                    val isCurrent = today.year == year && today.monthNumber == month
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(when { isFull -> Color(0xFF6FCF97).copy(alpha = 0.18f); isPartial -> Color(0xFFF2994A).copy(alpha = 0.18f); isCurrent -> AppColors.AccentGold.copy(alpha = 0.12f); else -> AppColors.Surface })
                            .border(1.dp, when { isCurrent -> AppColors.AccentGold.copy(alpha = 0.65f); isFull -> Color(0xFF6FCF97).copy(alpha = 0.45f); isPartial -> Color(0xFFF2994A).copy(alpha = 0.5f); else -> AppColors.Border }, RoundedCornerShape(8.dp))
                            .combinedClickable(onClick = { onNavigateToEdit(LocalDate(year, month, 1)) }, onLongClick = { onLongClick(monthKey) })
                            .heightIn(min = 62.dp)
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                shortMonthName(month),
                                color = when { isCurrent -> AppColors.AccentGold; isFull -> Color(0xFF6FCF97); isPartial -> Color(0xFFF2994A); else -> AppColors.TextMuted },
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent || isFull || isPartial) FontWeight.Medium else FontWeight.Normal,
                            )
                            if (isPartial) Text("$count/72", color = Color(0xFFF2994A).copy(alpha = 0.8f), fontSize = 10.sp, lineHeight = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

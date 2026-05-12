package com.astro.app.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import astroapp.composeapp.generated.resources.*
import com.astro.app.i18n.*
import com.astro.app.ui.components.*
import com.astro.app.ui.theme.*
import kotlinx.coroutines.launch


// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun localizedMonths() = listOf(
    str.onb_month_jan, str.onb_month_feb,
    str.onb_month_mar, str.onb_month_apr,
    str.onb_month_may, str.onb_month_jun,
    str.onb_month_jul, str.onb_month_aug,
    str.onb_month_sep, str.onb_month_oct,
    str.onb_month_nov, str.onb_month_dec,
)

private fun daysInMonth(month: Int, year: Int): Int = when (month) {
    2    -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAdmin: () -> Unit = {}
) {
    val state  by vm.state.collectAsState()
    val sign   = state.sign
    val elementColor = AppColors.elementColor(sign.element)
    val months = localizedMonths()

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ga"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xxl))

            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(Brush.radialGradient(
                        listOf(elementColor.copy(alpha = glowAlpha * 0.35f), AppColors.Background)
                    ))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(elementColor.copy(alpha = glowAlpha), elementColor.copy(alpha = glowAlpha * 0.4f))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = sign.iconPainter(),
                    contentDescription = sign.localizedName(),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(58.dp)
                )
            }

            Spacer(Modifier.height(Spacing.s))

            Text(
                text = if (state.name.isNotBlank()) state.name else sign.localizedName(),
                fontSize = AppType.h2, fontWeight = FontWeight.Light, color = AppColors.TextPrimary
            )

            if (state.birthDay > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter = sign.iconPainter(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${sign.localizedName()}  ·  ${state.birthDay} ${months[state.birthMonth - 1]} ${state.birthYear}",
                        fontSize = AppType.caption, color = AppColors.TextDim, textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(sign.localizedDates(), fontSize = AppType.caption, color = AppColors.TextDim)
            }

            Spacer(Modifier.height(Spacing.xl))

            // ── Form fields ───────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {

                // Имя
                ProfileCard(label = str.profile_field_name_label) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { vm.setName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(str.profile_field_name_placeholder, color = AppColors.TextDim, fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = LocalTextStyle.current.copy(color = AppColors.TextPrimary, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = AppColors.AccentGold,
                            unfocusedBorderColor    = if (state.name.isNotBlank()) AppColors.AccentGold else AppColors.Border,
                            cursorColor             = AppColors.AccentGold,
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(Radius.s)
                    )
                }

                // Пол
                ProfileCard(label = str.profile_field_gender_label) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GenderButton(
                            label    = str.profile_field_gender_male,
                            selected = state.gender == "male",
                            onClick  = { vm.setGender(if (state.gender == "male") "" else "male") },
                            modifier = Modifier.weight(1f)
                        )
                        GenderButton(
                            label    = str.profile_field_gender_female,
                            selected = state.gender == "female",
                            onClick  = { vm.setGender(if (state.gender == "female") "" else "female") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Дата рождения
                ProfileCard(label = str.profile_field_date_label) {
                    PickerRow(
                        value  = if (state.birthDay > 0)
                            "${state.birthDay} ${months[state.birthMonth - 1]} ${state.birthYear}"
                        else str.profile_field_date_pick,
                        icon   = "📅",
                        filled = state.birthDay > 0,
                        onClick = { vm.showDatePicker() }
                    )
                    AnimatedVisibility(
                        visible = state.birthDay > 0,
                        enter   = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit    = fadeOut(tween(200)) + shrinkVertically(tween(200))
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = sign.iconPainter(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(sign.localizedName(), fontSize = 12.sp, color = AppColors.AccentGold, fontWeight = FontWeight.Medium)
                            Text("·", color = AppColors.TextDim, fontSize = 12.sp)
                            Text(sign.localizedDates(), fontSize = 11.sp, color = AppColors.TextDim)
                        }
                    }
                }

                // Время рождения
                ProfileCard(label = str.profile_field_time_label) {
                    PickerRow(
                        value  = if (state.birthHour >= 0)
                            "${state.birthHour.toString().padStart(2, '0')}:${state.birthMinute.toString().padStart(2, '0')}"
                        else str.profile_field_time_pick,
                        icon   = "🕐",
                        filled = state.birthHour >= 0,
                        onClick = { vm.showTimePicker() }
                    )
                }

                // Место рождения
                ProfileCard(label = str.profile_field_place_label) {
                    PickerRow(
                        value  = state.birthPlace.ifBlank { str.profile_field_place_pick },
                        icon   = "📍",
                        filled = state.birthPlace.isNotBlank(),
                        onClick = { vm.showPlacePicker() }
                    )
                }

                // Язык
                ProfileCard(label = str.profile_field_language_label) {
                    LanguageDropdown(
                        selected  = state.language,
                        onSelect  = { vm.setLanguage(it) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // ── Stats row ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.m))
                    .background(AppColors.Card),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(str.profile_stat_element, sign.localizedElement(), elementColor)
                Box(Modifier.width(1.dp).height(48.dp).background(AppColors.Border).align(Alignment.CenterVertically))
                StatItem(str.profile_stat_planet, sign.localizedPlanet(), AppColors.AccentGold)
                Box(Modifier.width(1.dp).height(48.dp).background(AppColors.Border).align(Alignment.CenterVertically))
                StatItem(str.profile_stat_period, sign.localizedDates().split("–").first().trim(), AppColors.TextMuted)
            }

            Spacer(Modifier.height(Spacing.xl))
            if (state.name == "Admin_Iruna_864--33") {
                TextButton(onClick = onNavigateToAdmin) {
                    Text("Admin Panel", color = AppColors.AccentGold.copy(alpha = 0.45f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (state.showDatePicker) {
        DatePickerDialog(
            initialDay   = state.birthDay.coerceAtLeast(1),
            initialMonth = state.birthMonth.coerceAtLeast(1),
            initialYear  = state.birthYear,
            onConfirm    = { d, m, y -> vm.setBirthDate(d, m, y) },
            onDismiss    = { vm.hideDatePicker() }
        )
    }

    if (state.showTimePicker) {
        TimePickerDialog(
            initialHour   = if (state.birthHour >= 0) state.birthHour else 12,
            initialMinute = state.birthMinute,
            onConfirm     = { h, m -> vm.setBirthTime(h, m) },
            onDismiss     = { vm.hideTimePicker() }
        )
    }

    val pickerVm: PlacePickerViewModel = viewModel()

    if (state.showPlacePicker) {
        LaunchedEffect(Unit) { pickerVm.initialize(state.birthPlace, state.birthLat, state.birthLng) }
        PlacePickerDialog(
            vm        = pickerVm,
            onConfirm = { name, lat, lng -> vm.setPlace(name, lat, lng) },
            onDismiss = { vm.hidePlacePicker() }
        )
    }
}

// ── Card wrapper ──────────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .border(1.dp, AppColors.Border, RoundedCornerShape(Radius.m))
            .padding(horizontal = Spacing.l, vertical = Spacing.m)
    ) {
        Text(label, color = AppColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.7.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

// ── Picker row ────────────────────────────────────────────────────────────────

@Composable
private fun PickerRow(value: String, icon: String, filled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Radius.s))
            .background(AppColors.CardDark)
            .border(1.dp, if (filled) AppColors.AccentGold.copy(alpha = 0.35f) else AppColors.Border, RoundedCornerShape(Radius.s))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 16.sp)
        Text(value, fontSize = 14.sp, color = if (filled) AppColors.TextPrimary else AppColors.TextDim, modifier = Modifier.weight(1f))
        if (filled) Text("✎", fontSize = 12.sp, color = AppColors.AccentGold.copy(alpha = 0.6f))
    }
}

// ── Stat item ─────────────────────────────────────────────────────────────────

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = AppType.body, color = color, fontWeight = FontWeight.Normal)
        Text(label, fontSize = TextUnit(9f, TextUnitType.Sp), color = AppColors.TextDim)
    }
}

// ── Gender button ─────────────────────────────────────────────────────────────

@Composable
private fun GenderButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) AppColors.AccentGold else AppColors.Border
    val bgColor     = if (selected) AppColors.AccentGold.copy(alpha = 0.12f) else AppColors.CardDark
    val textColor   = if (selected) AppColors.AccentGold else AppColors.TextDim

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.s))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(Radius.s))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, color = textColor, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

// ── Language dropdown ─────────────────────────────────────────────────────────

@Composable
private fun LanguageDropdown(
    selected: AppLanguage,
    onSelect:  (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val arrowAngle by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label         = "langArrow"
    )

    val triggerShape = if (expanded)
        RoundedCornerShape(topStart = Radius.s, topEnd = Radius.s)
    else
        RoundedCornerShape(Radius.s)

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Trigger ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(triggerShape)
                .background(AppColors.CardDark)
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.35f), triggerShape)
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🌐", fontSize = 16.sp)
            Text(
                text     = selected.nativeName,
                fontSize = 14.sp,
                color    = AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text     = "▼",
                fontSize = 10.sp,
                color    = AppColors.AccentGold.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer { rotationZ = arrowAngle }
            )
        }

        // ── Options list ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn(tween(150)) + expandVertically(tween(200), expandFrom = Alignment.Top),
            exit    = fadeOut(tween(100)) + shrinkVertically(tween(150))
        ) {
            val listShape = RoundedCornerShape(bottomStart = Radius.s, bottomEnd = Radius.s)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(listShape)
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.AccentGold.copy(alpha = 0.18f), listShape)
            ) {
                AppLanguage.entries.forEachIndexed { index, lang ->
                    val isSelected = lang == selected

                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .padding(horizontal = 14.dp)
                                .background(AppColors.Border)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) AppColors.AccentGold.copy(alpha = 0.07f)
                                else Color.Transparent
                            )
                            .clickable {
                                expanded = false
                                onSelect(lang)
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Галочка для выбранного
                        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                            if (isSelected) Text("✓", fontSize = 12.sp, color = AppColors.AccentGold)
                        }
                        Text(
                            text       = lang.nativeName,
                            fontSize   = 15.sp,
                            color      = if (isSelected) AppColors.AccentGold else AppColors.TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ── Wheel picker ──────────────────────────────────────────────────────────────
//
// Принцип: вместо contentPadding добавляем (visibleCount/2) пустых элементов
// в начало и конец списка. Тогда:
//   - firstVisibleItemIndex == выбранный индекс в исходном items
//   - Центральная ячейка всегда визуально совпадает с firstVisible + halfVisible,
//     что = paddedItems[firstVisible + halfVisible] = items[firstVisible]
//   - Линии выделения рисуются ровно по центру Box → всё совпадает.
//
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeightDp: Dp = 46.dp,
    visibleCount: Int = 5,
) {
    val half  = visibleCount / 2
    val scope = rememberCoroutineScope()

    // Добавляем пустые слоты-отступы: items[i] → paddedItems[i + half]
    val padded = remember(items, half) {
        List(half) { "" } + items + List(half) { "" }
    }

    // initialFirstVisibleItemIndex = selectedIndex, потому что
    // padded[selectedIndex + half] = items[selectedIndex] окажется ровно по центру
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)
    )
    val fling = rememberSnapFlingBehavior(listState)

    // currentIdx = firstVisible, т.к. padded[firstVisible + half] = items[firstVisible]
    val currentIdx by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, items.size - 1) }
    }

    // Сообщаем наружу при изменении
    LaunchedEffect(currentIdx) {
        if (currentIdx != selectedIndex) onIndexChange(currentIdx)
    }

    // Доснап если пользователь остановил скролл без флинга
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && listState.firstVisibleItemScrollOffset != 0) {
            scope.launch { listState.animateScrollToItem(currentIdx) }
        }
    }

    Box(modifier = modifier.height(itemHeightDp * visibleCount)) {
        LazyColumn(
            state         = listState,
            flingBehavior = fling,
            modifier      = Modifier.fillMaxSize()
        ) {
            items(padded.size) { pi ->
                val realIdx = pi - half
                val isReal  = realIdx in 0 until items.size
                val dist    = if (isReal) kotlin.math.abs(realIdx - currentIdx) else half + 1

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp)
                        .alpha(when (dist) { 0 -> 1f; 1 -> 0.55f; 2 -> 0.25f; else -> 0.08f }),
                    contentAlignment = Alignment.Center
                ) {
                    if (isReal) {
                        Text(
                            text       = items[realIdx],
                            fontSize   = if (dist == 0) 17.sp else 14.sp,
                            fontWeight = if (dist == 0) FontWeight.Medium else FontWeight.Light,
                            color      = if (dist == 0) AppColors.TextPrimary else AppColors.TextMuted,
                            textAlign  = TextAlign.Center,
                            maxLines   = 1
                        )
                    }
                }
            }
        }

        // Линии выделения — ровно по центру Box
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeightDp)
                .padding(horizontal = 8.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                .background(AppColors.AccentGold.copy(alpha = 0.4f)))
            Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter)
                .background(AppColors.AccentGold.copy(alpha = 0.4f)))
        }
    }
}

// ── Dialog shell ──────────────────────────────────────────────────────────────

@Composable
private fun PickerDialogShell(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s))
                        .background(AppColors.Card).border(1.dp, AppColors.Border, RoundedCornerShape(Radius.s))
                        .clickable(onClick = onDismiss).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text(str.profile_dialog_cancel, color = AppColors.TextMuted, fontSize = 14.sp) }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(Radius.s))
                        .background(AppColors.AccentGold).clickable(onClick = onConfirm).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text(str.profile_dialog_confirm, color = Color(0xFF0A0A0F), fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

// ── Date picker dialog ────────────────────────────────────────────────────────

@Composable
private fun DatePickerDialog(
    initialDay: Int, initialMonth: Int, initialYear: Int,
    onConfirm: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var day   by remember { mutableStateOf(initialDay.coerceIn(1, 31)) }
    var month by remember { mutableStateOf(initialMonth.coerceIn(1, 12)) }
    var year  by remember { mutableStateOf(initialYear) }
    val months = localizedMonths()
    val days  = remember(month, year) { (1..daysInMonth(month, year)).map { it.toString() } }
    val years = (1930..2015).map { it.toString() }

    LaunchedEffect(month, year) {
        val max = daysInMonth(month, year)
        if (day > max) day = max
    }

    PickerDialogShell(title = "Дата рождения", onDismiss = onDismiss, onConfirm = { onConfirm(day, month, year) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WheelPicker(
                items         = days,
                selectedIndex = (day - 1).coerceIn(0, days.size - 1),
                onIndexChange = { day = it + 1 },
                modifier      = Modifier.weight(1f)
            )
            WheelPicker(
                items         = months,
                selectedIndex = (month - 1).coerceIn(0, 11),
                onIndexChange = { month = it + 1 },
                modifier      = Modifier.weight(2f)
            )
            WheelPicker(
                items         = years,
                selectedIndex = (year - 1930).coerceIn(0, years.size - 1),
                onIndexChange = { year = it + 1930 },
                modifier      = Modifier.weight(1.5f)
            )
        }
    }
}

// ── Time picker dialog ────────────────────────────────────────────────────────

@Composable
private fun TimePickerDialog(
    initialHour: Int, initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour   by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }

    PickerDialogShell(title = "Время рождения", onDismiss = onDismiss, onConfirm = { onConfirm(hour, minute) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WheelPicker(
                items = (0..23).map { it.toString().padStart(2, '0') },
                selectedIndex = hour,
                onIndexChange = { hour = it },
                modifier = Modifier.weight(1f)
            )
            Text(
                ":", fontSize = 24.sp, fontWeight = FontWeight.Light, color = AppColors.AccentGold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            WheelPicker(
                items = (0..59).map { it.toString().padStart(2, '0') },
                selectedIndex = minute,
                onIndexChange = { minute = it },
                modifier = Modifier.weight(1f)
            )
        }}}


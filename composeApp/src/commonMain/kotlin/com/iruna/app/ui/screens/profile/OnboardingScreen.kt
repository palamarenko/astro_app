package com.iruna.app.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iruna.app.i18n.format
import iruna.composeapp.generated.resources.*
import com.iruna.app.i18n.iconSmallPainter
import com.iruna.app.i18n.str
import com.iruna.app.ui.components.StarfieldBackground
import com.iruna.app.ui.theme.*


// ── Constants ─────────────────────────────────────────────────────────────────

/**
 * Шаги онбординга:
 *  0 — Приветствие
 *  1 — Имя
 *  2 — Пол
 *  3 — Дата рождения
 *  4 — Время рождения
 *  5 — Место рождения
 *  6 — Финальный экран
 */
private const val ONBOARDING_TOTAL_STEPS = 7

@Composable
private fun monthNames(): List<String> = listOf(
    str.onb_month_jan,
    str.onb_month_feb,
    str.onb_month_mar,
    str.onb_month_apr,
    str.onb_month_may,
    str.onb_month_jun,
    str.onb_month_jul,
    str.onb_month_aug,
    str.onb_month_sep,
    str.onb_month_oct,
    str.onb_month_nov,
    str.onb_month_dec,
)

private fun daysInMonthOnb(month: Int, year: Int): Int = when (month) {
    2    -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

// ── Main Screen ───────────────────────────────────────────────────────────────

/**
 * Полноэкранный онбординг.
 *
 * Состояние:
 *  - текущий шаг хранится в [ProfileViewModel.onboardingStep] и сохраняется
 *    при каждом переходе → возобновление при следующем запуске.
 *  - после завершения ставится флаг [ProfileViewModel.onboardingFinished],
 *    после чего онбординг больше не показывается.
 *
 * @param onFinished колбэк, вызывается когда онбординг закрывается (завершён или пропущен).
 */
@Composable
fun OnboardingScreen(
    vm: ProfileViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()

    // Текущий шаг = тот, на котором остановился пользователь, но в пределах [0, TOTAL-1]
    var step by remember { mutableStateOf(state.onboardingStep.coerceIn(0, ONBOARDING_TOTAL_STEPS - 1)) }
    // Направление перехода для AnimatedContent
    var direction by remember { mutableStateOf(1) }

    // Сохраняем шаг в storage при каждом изменении
    LaunchedEffect(step) {
        if (state.onboardingStep != step) vm.setOnboardingStep(step)
    }

    fun goNext() {
        if (step < ONBOARDING_TOTAL_STEPS - 1) {
            direction = 1
            step += 1
        } else {
            vm.finishOnboarding()
            onFinished()
        }
    }

    fun goBack() {
        if (step > 0) {
            direction = -1
            step -= 1
        }
    }

    fun finishAll() {
        vm.finishOnboarding()
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF12121F),
                        AppColors.Background,
                        Color(0xFF080810),
                    )
                )
            )
    ) {
        StarfieldBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = Spacing.xl)
        ) {
            // ── Top bar: прогресс + skip ──────────────────────────────────────
            Spacer(Modifier.height(Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка "назад"
                AnimatedVisibility(visible = step in 1 until (ONBOARDING_TOTAL_STEPS - 1)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppColors.Card.copy(alpha = 0.5f))
                            .border(1.dp, AppColors.Border, CircleShape)
                            .clickable { goBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("‹", color = AppColors.TextSecondary, fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(Spacing.m))

                // Сегменты прогресса
                StepIndicator(
                    total = ONBOARDING_TOTAL_STEPS,
                    current = step,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                )

                Spacer(Modifier.width(Spacing.m))

                // Skip-all справа (кроме финального шага)
                if (step < ONBOARDING_TOTAL_STEPS - 1) {
                    TextButton(onClick = { finishAll() }) {
                        Text(
                            text = str.onb_btn_skip_all,
                            fontSize = 12.sp,
                            color = AppColors.TextDim,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.l))

            // ── Содержимое шага с анимацией ───────────────────────────────────
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val slide = 80
                    val enter = slideInHorizontally(
                        animationSpec = tween(360, easing = FastOutSlowInEasing),
                        initialOffsetX = { direction * slide }
                    ) + fadeIn(tween(360))

                    val exit = slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { -direction * slide }
                    ) + fadeOut(tween(220))

                    enter togetherWith exit
                },
                label = "onboardingStep",
                modifier = Modifier.weight(1f)
            ) { s ->
                when (s) {
                    0 -> WelcomeStep()
                    1 -> NameStep(state.name, vm::setName)
                    2 -> GenderStep(state.gender, vm::setGender)
                    3 -> DateStep(state, vm::setBirthDate)
                    4 -> TimeStep(state, vm::setBirthTime)
                    5 -> PlaceStep(vm)
                    6 -> FinalStep(state)
                    else -> {}
                }
            }

            // ── Bottom bar: кнопки ────────────────────────────────────────────
            BottomActions(
                step = step,
                state = state,
                onNext = { goNext() },
                onSkip = { goNext() },
                onFinish = { finishAll() }
            )

            Spacer(Modifier.height(Spacing.l))
        }
    }
}

// ── Step indicator (segments) ─────────────────────────────────────────────────

@Composable
private fun StepIndicator(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val active = i <= current
            val width by animateFloatAsState(
                targetValue = if (i == current) 1.6f else 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "segW$i"
            )
            val color by animateColorAsState(
                targetValue = when {
                    i < current  -> AppColors.AccentGold
                    i == current -> AppColors.AccentGold
                    else         -> AppColors.Border
                },
                animationSpec = tween(400),
                label = "segC$i"
            )
            Box(
                modifier = Modifier
                    .weight(width)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = if (active) 1f else 0.6f))
            )
        }
    }
}

// ── Bottom actions ────────────────────────────────────────────────────────────

@Composable
private fun BottomActions(
    step: Int,
    state: ProfileUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    val isLast = step == ONBOARDING_TOTAL_STEPS - 1
    val isWelcome = step == 0

    val startLabel = str.onb_btn_start
    val nextLabel  = str.onb_btn_next
    val doneLabel  = str.onb_btn_done

    // Метка кнопки и условие активности
    val (label, isFilled) = when (step) {
        0 -> startLabel to true
        1 -> nextLabel  to state.name.isNotBlank()
        2 -> nextLabel  to state.gender.isNotBlank()
        3 -> nextLabel  to (state.birthDay > 0)
        4 -> nextLabel  to (state.birthHour >= 0)
        5 -> nextLabel  to state.birthPlace.isNotBlank()
        6 -> doneLabel  to true
        else -> nextLabel to true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Главная кнопка — золотая. Пульсирует, когда поле заполнено.
        OnboardingPrimaryButton(
            text = label,
            highlighted = isFilled,
            onClick = { if (isLast) onFinish() else onNext() }
        )

        // Skip-this-step (мелкая ссылка), только на шагах ввода
        if (!isWelcome && !isLast) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = str.onb_btn_skip_step,
                        fontSize = 12.sp,
                        color = AppColors.TextMuted,
                    )
                }
            }
        } else {
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun OnboardingPrimaryButton(
    text: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val glow by rememberInfiniteTransition(label = "btnGlow").animateFloat(
        initialValue = 0.45f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnGlowA"
    )

    val baseAlpha = if (highlighted) glow else 0.35f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        AppColors.AccentGold.copy(alpha = if (highlighted) 0.22f else 0.08f),
                        AppColors.AccentGold.copy(alpha = if (highlighted) 0.12f else 0.04f),
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        AppColors.AccentGold.copy(alpha = baseAlpha),
                        AppColors.AccentGold.copy(alpha = baseAlpha * 0.55f),
                    )
                ),
                shape = RoundedCornerShape(Radius.xl)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = AppType.bodyLg,
            fontWeight = FontWeight.Medium,
            color = AppColors.AccentGold,
            letterSpacing = TextUnit(0.06f, TextUnitType.Em),
        )
    }
}

// ── Step 0: Welcome ───────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep() {
    val pulse by rememberInfiniteTransition(label = "wpulse").animateFloat(
        initialValue = 0.85f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wpulseA"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AppColors.AccentGold.copy(alpha = 0.25f),
                            AppColors.AccentGold.copy(alpha = 0.05f),
                            Color.Transparent,
                        )
                    )
                )
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✶", fontSize = 64.sp, color = AppColors.AccentGold)
        }

        Spacer(Modifier.height(Spacing.xxl))

        Text(
            text = str.onb_welcome_title,
            fontSize = AppType.h1,
            fontWeight = FontWeight.Light,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = str.onb_welcome_subtitle,
            fontSize = AppType.title,
            fontWeight = FontWeight.Light,
            color = AppColors.AccentGold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = str.onb_welcome_desc,
            fontSize = AppType.body,
            color = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ── Step 1: Name ──────────────────────────────────────────────────────────────

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit) {
    StepContainer(
        emoji = "✦",
        title = str.onb_name_title,
        subtitle = str.onb_name_subtitle
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(str.onb_name_placeholder, color = AppColors.TextDim, fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextPrimary, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = AppColors.AccentGold,
                unfocusedBorderColor    = if (name.isNotBlank()) AppColors.AccentGold.copy(alpha = 0.6f) else AppColors.Border,
                cursorColor             = AppColors.AccentGold,
                focusedContainerColor   = AppColors.Card.copy(alpha = 0.4f),
                unfocusedContainerColor = AppColors.Card.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(Radius.m)
        )
    }
}

// ── Step 2: Gender ────────────────────────────────────────────────────────────

@Composable
private fun GenderStep(gender: String, onGenderChange: (String) -> Unit) {
    StepContainer(
        emoji = "☯",
        title = str.onb_gender_title,
        subtitle = str.onb_gender_subtitle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigGenderCard(
                emoji = "♂",
                label = str.onb_gender_male,
                selected = gender == "male",
                onClick = { onGenderChange(if (gender == "male") "" else "male") },
                modifier = Modifier.weight(1f)
            )
            BigGenderCard(
                emoji = "♀",
                label = str.onb_gender_female,
                selected = gender == "female",
                onClick = { onGenderChange(if (gender == "female") "" else "female") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BigGenderCard(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) AppColors.AccentGold else AppColors.Border,
        animationSpec = tween(220), label = "gbBorder"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) AppColors.AccentGold.copy(alpha = 0.12f) else AppColors.Card.copy(alpha = 0.4f),
        animationSpec = tween(220), label = "gbBg"
    )
    val emojiColor by animateColorAsState(
        targetValue = if (selected) AppColors.AccentGold else AppColors.TextMuted,
        animationSpec = tween(220), label = "gbE"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.l))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(Radius.l))
            .clickable(onClick = onClick)
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 40.sp, color = emojiColor)
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 15.sp,
                color = if (selected) AppColors.TextPrimary else AppColors.TextMuted,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

// ── Step 3: Date ──────────────────────────────────────────────────────────────

@Composable
private fun DateStep(
    state: ProfileUiState,
    onConfirm: (Int, Int, Int) -> Unit,
) {
    val hadDate = state.birthDay > 0
    var day   by remember { mutableStateOf(if (hadDate) state.birthDay else 15) }
    var month by remember { mutableStateOf(if (hadDate) state.birthMonth else 6) }
    var year  by remember { mutableStateOf(state.birthYear) }
    // Не сохраняем дефолты автоматически — только после явного взаимодействия пользователя.
    var touched by remember { mutableStateOf(hadDate) }

    // Корректируем день при смене месяца/года
    LaunchedEffect(month, year) {
        val max = daysInMonthOnb(month, year)
        if (day > max) { day = max; touched = true }
    }

    // Сохраняем только если пользователь крутил пикеры (или дата уже была сохранена ранее)
    LaunchedEffect(day, month, year, touched) {
        if (touched) onConfirm(day, month, year)
    }

    StepContainer(
        emoji = "☽",
        title = str.onb_date_title,
        subtitle = str.onb_date_subtitle
    ) {
        val months = monthNames()
        val days  = remember(month, year) { (1..daysInMonthOnb(month, year)).map { it.toString() } }
        val years = (1930..2015).map { it.toString() }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WheelPicker(
                items         = days,
                selectedIndex = (day - 1).coerceIn(0, days.size - 1),
                onIndexChange = { day = it + 1; touched = true },
                modifier      = Modifier.weight(1f)
            )
            WheelPicker(
                items         = months,
                selectedIndex = (month - 1).coerceIn(0, 11),
                onIndexChange = { month = it + 1; touched = true },
                modifier      = Modifier.weight(2f)
            )
            WheelPicker(
                items         = years,
                selectedIndex = (year - 1930).coerceIn(0, years.size - 1),
                onIndexChange = { year = it + 1930; touched = true },
                modifier      = Modifier.weight(1.5f)
            )
        }
    }
}

// ── Step 4: Time ──────────────────────────────────────────────────────────────

@Composable
private fun TimeStep(
    state: ProfileUiState,
    onConfirm: (Int, Int) -> Unit,
) {
    val hadTime = state.birthHour >= 0
    var hour   by remember { mutableStateOf(if (hadTime) state.birthHour else 12) }
    var minute by remember { mutableStateOf(state.birthMinute.coerceIn(0, 59)) }
    var touched by remember { mutableStateOf(hadTime) }

    LaunchedEffect(hour, minute, touched) {
        if (touched) onConfirm(hour, minute)
    }

    StepContainer(
        emoji = "⊙",
        title = str.onb_time_title,
        subtitle = str.onb_time_subtitle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WheelPicker(
                items         = (0..23).map { it.toString().padStart(2, '0') },
                selectedIndex = hour,
                onIndexChange = { hour = it; touched = true },
                modifier      = Modifier.weight(1f)
            )
            Text(
                ":", fontSize = 28.sp, fontWeight = FontWeight.Light, color = AppColors.AccentGold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            WheelPicker(
                items         = (0..59).map { it.toString().padStart(2, '0') },
                selectedIndex = minute,
                onIndexChange = { minute = it; touched = true },
                modifier      = Modifier.weight(1f)
            )
        }
    }
}

// ── Step 5: Place ─────────────────────────────────────────────────────────────

@Composable
private fun PlaceStep(vm: ProfileViewModel) {
    val state by vm.state.collectAsState()
    val pickerVm = viewModel { PlacePickerViewModel() }

    StepContainer(
        emoji = "⊕",
        title = str.onb_place_title,
        subtitle = str.onb_place_subtitle
    ) {
        val placeholderText = str.onb_place_placeholder
        // Карточка-кнопка для открытия пикера
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.m))
                .background(AppColors.Card.copy(alpha = 0.5f))
                .border(
                    1.dp,
                    if (state.birthPlace.isNotBlank()) AppColors.AccentGold.copy(alpha = 0.5f)
                    else AppColors.Border,
                    RoundedCornerShape(Radius.m)
                )
                .clickable { vm.showPlacePicker() }
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⊕", fontSize = 18.sp, color = AppColors.AccentGold)
                Text(
                    text = if (state.birthPlace.isBlank()) placeholderText
                           else state.birthPlace,
                    fontSize = 15.sp,
                    color = if (state.birthPlace.isBlank()) AppColors.TextDim
                            else AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (state.birthPlace.isBlank()) "→" else "✎",
                    color = AppColors.AccentGold.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }

    if (state.showPlacePicker) {
        LaunchedEffect(Unit) { pickerVm.initialize(state.birthPlace, state.birthLat, state.birthLng) }
        PlacePickerDialog(
            vm        = pickerVm,
            onConfirm = { name, lat, lng -> vm.setPlace(name, lat, lng) },
            onDismiss = { vm.hidePlacePicker() }
        )
    }
}

// ── Step 6: Final ─────────────────────────────────────────────────────────────

@Composable
private fun FinalStep(state: ProfileUiState) {
    val sign = state.sign
    val elementColor = AppColors.elementColor(sign.element)

    val glow by rememberInfiniteTransition(label = "fglow").animateFloat(
        initialValue = 0.4f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fglowA"
    )

    val finalTitle      = str.onb_final_title
    val finalSignName   = str.onb_final_sign_with_name.format(state.name)
    val finalSign       = str.onb_final_sign
    val summaryName     = str.onb_summary_name
    val summaryGender   = str.onb_summary_gender
    val genderMale      = str.onb_gender_male
    val genderFemale    = str.onb_gender_female
    val summaryDate     = str.onb_summary_birth_date
    val summaryTime     = str.onb_summary_birth_time
    val summaryPlace    = str.onb_summary_place
    val months          = monthNames()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = finalTitle,
            fontSize = AppType.h1,
            fontWeight = FontWeight.Light,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.name.isNotBlank()) finalSignName else finalSign,
            fontSize = AppType.body,
            color = AppColors.TextMuted
        )

        Spacer(Modifier.height(Spacing.xl))

        // Большой круг со знаком
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            elementColor.copy(alpha = glow * 0.4f),
                            elementColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            elementColor.copy(alpha = glow),
                            elementColor.copy(alpha = glow * 0.4f)
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = sign.iconSmallPainter(),
                contentDescription = sign.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(152.dp)
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = sign.name,
            fontSize = AppType.h2,
            fontWeight = FontWeight.Light,
            color = elementColor
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${sign.dates}  ·  ${sign.element}  ·  ${sign.planet}",
            fontSize = AppType.caption,
            color = AppColors.TextMuted
        )

        Spacer(Modifier.height(Spacing.xxl))

        // Сводка заполненного
        Column(
            modifier = Modifier.fillMaxWidth(0.92f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryRow(summaryName, if (state.name.isNotBlank()) state.name else "—")
            SummaryRow(summaryGender, when (state.gender) {
                "male"   -> genderMale
                "female" -> genderFemale
                else     -> "—"
            })
            SummaryRow(
                summaryDate,
                if (state.birthDay > 0)
                    "${state.birthDay} ${months[state.birthMonth - 1]} ${state.birthYear}"
                else "—"
            )
            SummaryRow(
                summaryTime,
                if (state.birthHour >= 0)
                    "${state.birthHour.toString().padStart(2, '0')}:${state.birthMinute.toString().padStart(2, '0')}"
                else "—"
            )
            SummaryRow(summaryPlace, state.birthPlace.ifBlank { "—" })
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.s))
            .background(AppColors.Card.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = AppColors.TextDim)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            color = if (value == "—") AppColors.TextDim else AppColors.TextPrimary,
            fontWeight = FontWeight.Normal
        )
    }
}

// ── Step container (shared header) ────────────────────────────────────────────

@Composable
private fun StepContainer(
    emoji: String,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(Spacing.l))

        // Эмодзи в круге
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AppColors.AccentGold.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 28.sp)
        }

        Spacer(Modifier.height(Spacing.l))

        Text(
            text = title,
            fontSize = AppType.title,
            fontWeight = FontWeight.Light,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = AppType.body,
            color = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth(0.92f)
        )

        Spacer(Modifier.height(Spacing.xxl))

        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}


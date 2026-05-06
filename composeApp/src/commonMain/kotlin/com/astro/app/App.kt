package com.astro.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import astroapp.composeapp.generated.resources.*
import com.astro.app.data.*
import com.astro.app.i18n.*
import com.astro.app.ui.components.CompatibilityNavIcon
import com.astro.app.ui.components.HoroscopeNavIcon
import com.astro.app.ui.components.TarotNavIcon
import com.astro.app.ui.screens.*
import com.astro.app.ui.theme.*
import org.jetbrains.compose.resources.stringResource

// Порядок табов для определения направления слайда
private val TAB_ORDER = listOf(
    BottomTab.HOROSCOPE,
    BottomTab.TAROT,
    BottomTab.COMPATIBILITY,
    BottomTab.PROFILE,
)

@Composable
fun App() {
    AppContent()
}

@Composable
private fun AppContent() {
    val api = remember { ClaudeApiClient(anthropicApiKey) }

    val horoscopeVm = remember { HoroscopeViewModel() }
    val tarotVm     = remember { TarotViewModel(api) }
    val compatVm    = remember { CompatibilityViewModel(api) }
    val profileVm   = remember { ProfileViewModel(api) }
    val adminVm      = remember { AdminViewModel(api) }
    val adminTarotVm = remember { AdminTarotViewModel(api) }

    var activeTab    by remember { mutableStateOf(BottomTab.HOROSCOPE) }
    var previousTab  by remember { mutableStateOf(BottomTab.HOROSCOPE) }
    // На главной сразу открыт детальный гороскоп выбранного знака
    var showDetail      by remember { mutableStateOf(true) }
    var showAdmin       by remember { mutableStateOf(false) }
    var showAdminTarot  by remember { mutableStateOf(false) }

    // Текущий выбранный знак (источник истины — ProfileViewModel)
    val profileState by profileVm.state.collectAsState()
    val selectedSign = profileState.sign

    // Синхронизируем выбранный знак с гороскопом (в т.ч. при первом запуске)
    LaunchedEffect(selectedSign) {
        if (horoscopeVm.state.value.selectedSign != selectedSign) {
            horoscopeVm.selectSign(selectedSign)
        }
    }

    // ── Онбординг ─────────────────────────────────────────────────────────────
    // Показываем, если пользователь его ещё не завершил/не пропустил полностью.
    // Если онбординг был прерван — возобновляем с сохранённого шага.
    if (!profileState.onboardingFinished) {
        OnboardingScreen(
            vm = profileVm,
            onFinished = { /* флаг уже выставлен внутри VM, перерисовка произойдёт автоматически */ },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Направление: +1 = вправо (новый таб правее), -1 = влево
    val direction = remember(activeTab) {
        val prev = TAB_ORDER.indexOf(previousTab)
        val curr = TAB_ORDER.indexOf(activeTab)
        if (curr >= prev) 1 else -1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp)) {
            AnimatedContent(
                targetState = activeTab to showDetail,
                transitionSpec = {
                    val slideOffset = 60
                    val enterSlide = slideInHorizontally(
                        animationSpec = tween(320, easing = FastOutSlowInEasing),
                        initialOffsetX = { direction * slideOffset }
                    ) + fadeIn(tween(320))

                    val exitSlide = slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { -direction * slideOffset }
                    ) + fadeOut(tween(200))

                    enterSlide togetherWith exitSlide
                },
                label = "tabContent"
            ) { (tab, detail) ->
                when (tab) {
                    BottomTab.HOROSCOPE -> {
                        if (detail) {
                            HoroscopeScreen(
                                vm = horoscopeVm,
                                onBack = { showDetail = false },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SignPickerScreen(
                                onSignSelected = { sign ->
                                    // Обновляем знак и в профиле, и в гороскопе
                                    profileVm.selectSign(sign)
                                    horoscopeVm.selectSign(sign)
                                    showDetail = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    BottomTab.TAROT         -> TarotScreen(vm = tarotVm, modifier = Modifier.fillMaxSize())
                    BottomTab.COMPATIBILITY -> ComingSoonScreen(modifier = Modifier.fillMaxSize())
                    BottomTab.PROFILE       -> {
                        when {
                            showAdminTarot -> AdminTarotScreen(
                                vm = adminTarotVm,
                                onNavigateBack = { showAdminTarot = false },
                                onNavigateToHoroscopes = { showAdminTarot = false; showAdmin = true }
                            )
                            showAdmin -> AdminScreen(
                                vm = adminVm,
                                onNavigateBack = { showAdmin = false },
                                onNavigateToTarot = { showAdmin = false; showAdminTarot = true }
                            )
                            else -> ProfileScreen(
                                vm = profileVm,
                                modifier = Modifier.fillMaxSize(),
                                onNavigateToAdmin = { showAdmin = true }
                            )
                        }
                    }
                }
            }
        }

        BottomNav(
            activeTab = activeTab,
            profileIcon = selectedSign.emoji,
            onTabSelected = { tab ->
                previousTab = activeTab
                if (tab == BottomTab.HOROSCOPE) showDetail = true
                if (tab != BottomTab.PROFILE) { showAdmin = false; showAdminTarot = false }
                activeTab = tab
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Bottom navigation ─────────────────────────────────────────────────────────
private data class NavItem(
    val tab: BottomTab,
    val label: String,
    val icon: @Composable (color: Color) -> Unit,
)

@Composable
private fun BottomNav(
    activeTab: BottomTab,
    profileIcon: String,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navItems = listOf(
        NavItem(BottomTab.HOROSCOPE,     stringResource(Res.string.nav_horoscope))     { c -> HoroscopeNavIcon(color = c) },
        NavItem(BottomTab.TAROT,         stringResource(Res.string.nav_tarot))         { c -> TarotNavIcon(color = c) },
        NavItem(BottomTab.COMPATIBILITY, stringResource(Res.string.nav_compatibility)) { c -> CompatibilityNavIcon(color = c) },
        NavItem(BottomTab.PROFILE,       stringResource(Res.string.nav_profile))       { c ->
            // Профайл — эмодзи выбранного знака
            Text(
                text = profileIcon,
                fontSize = TextUnit(20f, TextUnitType.Sp),
                color = c,
            )
        },
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(AppColors.NavBackground)
            .border(1.dp, AppColors.BorderDark, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
            navItems.forEach { item ->
                val isActive = item.tab == activeTab

                // Анимированный цвет иконки и текста
                val color by animateColorAsState(
                    targetValue = if (isActive) AppColors.AccentGold else AppColors.TextDim,
                    animationSpec = tween(250),
                    label = "navColor_${item.tab}"
                )

                // Анимированный масштаб при нажатии
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navScale_${item.tab}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Иконка с bounce-масштабом
                    Box(modifier = Modifier.scale(scale)) {
                        item.icon(color)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        color = color,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

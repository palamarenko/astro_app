package com.iruna.app

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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import iruna.composeapp.generated.resources.*
import com.iruna.app.data.*
import com.iruna.app.i18n.*
import com.iruna.app.ui.components.AppBackHandler
import com.iruna.app.ui.components.CompatibilityNavIcon
import com.iruna.app.ui.components.HoroscopeNavIcon
import com.iruna.app.ui.components.StarfieldBackground
import com.iruna.app.ui.components.TarotNavIcon
import com.iruna.app.ui.screens.*
import com.iruna.app.ui.screens.horoscope.*
import com.iruna.app.ui.screens.tarot.*
import com.iruna.app.ui.screens.compatibility.*
import com.iruna.app.ui.screens.profile.*
import com.iruna.app.ui.screens.admin.*
import com.iruna.app.ui.theme.*


// Порядок табов для определения направления слайда
private val TAB_ORDER = listOf(
    BottomTab.HOROSCOPE,
    BottomTab.TAROT,
    BottomTab.COMPATIBILITY,
    BottomTab.PROFILE,
)

@Composable
fun App() {
    // Инициализация языка — один раз при старте приложения
    remember {
        val saved = UserStorage.load()?.language
        LanguageManager.init(saved)
    }

    val lang by LanguageManager.language.collectAsState()

    // ViewModel-ы создаются один раз и живут вне key(lang),
    // чтобы смена языка не пересоздавала их (иначе init-блоки
    // срабатывают повторно и показывают, например, попап уведомлений).
    val api          = remember { ClaudeApiClient(anthropicApiKey) }
    val horoscopeVm  = remember { HoroscopeViewModel() }
    val tarotVm      = remember { TarotViewModel(api) }
    val compatVm     = remember { CompatibilityViewModel(api) }
    val profileVm    = remember { ProfileViewModel(api) }
    val adminVm      = remember { AdminViewModel(api) }
    val adminTarotVm = remember { AdminTarotViewModel(api) }

    // key(lang) заставляет Compose полностью пересоздать AppContent() при смене языка,
    // чтобы все str.xxx вызовы вернули строки нового языка.
    key(lang) {
        AppContent(
            horoscopeVm  = horoscopeVm,
            tarotVm      = tarotVm,
            compatVm     = compatVm,
            profileVm    = profileVm,
            adminVm      = adminVm,
            adminTarotVm = adminTarotVm,
        )
    }
}

@Composable
private fun AppContent(
    horoscopeVm:  HoroscopeViewModel,
    tarotVm:      TarotViewModel,
    compatVm:     CompatibilityViewModel,
    profileVm:    ProfileViewModel,
    adminVm:      AdminViewModel,
    adminTarotVm: AdminTarotViewModel,
) {

    var activeTab    by remember { mutableStateOf(BottomTab.HOROSCOPE) }
    // На главной сразу открыт детальный гороскоп выбранного знака
    var showDetail      by remember { mutableStateOf(true) }
    var showAdmin              by remember { mutableStateOf(false) }
    var showAdminTarot         by remember { mutableStateOf(false) }
    var showAdminNotifications by remember { mutableStateOf(false) }
    var showAdminSettings      by remember { mutableStateOf(false) }
    // Таро: выбранный период (null = список)
    var tarotPeriod     by remember { mutableStateOf<HoroscopePeriod?>(null) }

    // ── Системная кнопка «Назад» ──────────────────────────────────────────────
    // • Таро-расклад           → список таро
    // • Любой другой экран     → гороскоп
    // • Гороскоп (главный)     → не перехватываем (приложение закрывается)
    val onHoroscope = activeTab == BottomTab.HOROSCOPE && showDetail && !showAdmin && !showAdminTarot && !showAdminNotifications && !showAdminSettings
    AppBackHandler(enabled = !onHoroscope) {
        when {
            // Таро-расклад → список
            activeTab == BottomTab.TAROT && tarotPeriod != null -> {
                tarotVm.clearPeriod()
                tarotPeriod = null
            }
            // Настройки → назад
            showAdminSettings -> { showAdminSettings = false }
            // Админ-панель уведомлений → назад
            showAdminNotifications -> { showAdminNotifications = false }
            // Админ-панель таро → назад
            showAdminTarot -> { showAdminTarot = false }
            // Главная админ-панель → назад
            showAdmin -> { showAdmin = false }
            // Все остальные экраны → гороскоп
            else -> {
                activeTab = BottomTab.HOROSCOPE
                showDetail = true
                showAdmin = false
                showAdminTarot = false
                showAdminNotifications = false
                showAdminSettings = false
                tarotPeriod = null
                tarotVm.clearPeriod()
            }
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Глобальный фон — мерцающие звёзды поверх тёмного фона
        StarfieldBackground(modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp)) {
            AnimatedContent(
                targetState = Triple(activeTab, showDetail, tarotPeriod),
                transitionSpec = {
                    val (prevTab, prevDetail, prevPeriod) = initialState
                    val (currTab, currDetail, currPeriod) = targetState

                    // Определяем направление слайда по тому, что именно изменилось
                    val dir: Int = when {
                        prevTab != currTab -> {
                            val p = TAB_ORDER.indexOf(prevTab)
                            val c = TAB_ORDER.indexOf(currTab)
                            if (c >= p) 1 else -1
                        }
                        prevPeriod == null && currPeriod != null ->  1   // вперёд: список → расклад
                        prevPeriod != null && currPeriod == null -> -1   // назад:  расклад → список
                        !prevDetail && currDetail ->  1
                        prevDetail && !currDetail -> -1
                        else -> 1
                    }

                    val slideOffset = 60
                    slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { dir * slideOffset } +
                        fadeIn(tween(320)) togetherWith
                    slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -dir * slideOffset } +
                        fadeOut(tween(200))
                },
                label = "tabContent"
            ) { (tab, detail, period) ->
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
                    BottomTab.TAROT         -> {
                        if (period == null) {
                            TarotListScreen(
                                vm = tarotVm,
                                onPeriodSelected = { selectedPeriod ->
                                    tarotVm.selectPeriod(selectedPeriod)
                                    tarotPeriod = selectedPeriod
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            TarotReadingScreen(
                                vm = tarotVm,
                                onBack = {
                                    tarotVm.clearPeriod()
                                    tarotPeriod = null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    BottomTab.COMPATIBILITY -> ComingSoonScreen(modifier = Modifier.fillMaxSize())

                    BottomTab.PROFILE       -> {
                        when {
                            showAdminSettings -> AdminSettingsScreen(
                                vm = adminVm,
                                onNavigateBack = { showAdminSettings = false },
                                onNavigateToHoroscopes = { showAdminSettings = false; showAdmin = true },
                                onNavigateToTarot = { showAdminSettings = false; showAdminTarot = true },
                                onNavigateToNotifications = { showAdminSettings = false; showAdminNotifications = true },
                            )
                            showAdminNotifications -> AdminNotificationsScreen(
                                vm = adminVm,
                                onNavigateBack = { showAdminNotifications = false },
                                onNavigateToHoroscopes = { showAdminNotifications = false; showAdmin = true },
                                onNavigateToTarot = { showAdminNotifications = false; showAdminTarot = true },
                                onNavigateToSettings = { showAdminNotifications = false; showAdminSettings = true },
                            )
                            showAdminTarot -> AdminTarotScreen(
                                vm = adminTarotVm,
                                adminVm = adminVm,
                                onNavigateBack = { showAdminTarot = false },
                                onNavigateToHoroscopes = { showAdminTarot = false; showAdmin = true },
                                onNavigateToNotifications = { showAdminTarot = false; showAdminNotifications = true },
                                onNavigateToSettings = { showAdminTarot = false; showAdminSettings = true },
                            )
                            showAdmin -> AdminScreen(
                                vm = adminVm,
                                onNavigateBack = { showAdmin = false },
                                onNavigateToTarot = { showAdmin = false; showAdminTarot = true },
                                onNavigateToNotifications = { showAdmin = false; showAdminNotifications = true },
                                onNavigateToSettings = { showAdmin = false; showAdminSettings = true },
                            )
                            else -> ProfileScreen(
                                vm = profileVm,
                                modifier = Modifier.fillMaxSize(),
                                onNavigateToAdmin = { showAdmin = true },
                            )
                        }
                    }

                    else -> {}
                }
            }
        }

        BottomNav(
            activeTab = activeTab,
            selectedSign = selectedSign,
            onTabSelected = { tab ->
                if (tab == BottomTab.HOROSCOPE) showDetail = true
                if (tab != BottomTab.PROFILE) { showAdmin = false; showAdminTarot = false; showAdminNotifications = false; showAdminSettings = false }
                if (tab != BottomTab.TAROT) { tarotPeriod = null; tarotVm.clearPeriod() }
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
    selectedSign: ZodiacSign,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navItems = listOf(
        NavItem(BottomTab.HOROSCOPE,      str.nav_horoscope)      { c -> HoroscopeNavIcon(color = c) },
        NavItem(BottomTab.TAROT,          str.nav_tarot)          { c -> TarotNavIcon(color = c) },
        NavItem(BottomTab.COMPATIBILITY,  str.nav_compatibility)  { c -> CompatibilityNavIcon(color = c) },
        NavItem(BottomTab.PROFILE,        str.nav_profile)        { _ ->
            Image(
                painter = selectedSign.iconPainter(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp),
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
                        .fillMaxHeight()
                        .clickable { onTabSelected(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
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

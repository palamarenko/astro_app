package com.iruna.app.ui.screens.tarot

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import iruna.composeapp.generated.resources.*
import com.iruna.app.data.HoroscopePeriod
import com.iruna.app.data.TarotCard
import com.iruna.app.data.ALL_TAROT
import com.iruna.app.i18n.str
import com.iruna.app.ui.components.SectionLabel
import com.iruna.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource


// ── Маппинг resourceKey → DrawableResource ────────────────────────────────────

@Composable
private fun TarotCard.painter() = painterResource(
    when (resourceKey) {
        "fool"             -> Res.drawable.tarot_fool
        "magician"         -> Res.drawable.tarot_magician
        "high_priestess"   -> Res.drawable.tarot_high_priestess
        "empress"          -> Res.drawable.tarot_empress
        "emperor"          -> Res.drawable.tarot_emperor
        "hierophant"       -> Res.drawable.tarot_hierophant
        "lovers"           -> Res.drawable.tarot_lovers
        "chariot"          -> Res.drawable.tarot_chariot
        "strength"         -> Res.drawable.tarot_strength
        "hermit"           -> Res.drawable.tarot_hermit
        "wheel_of_fortune" -> Res.drawable.tarot_wheel_of_fortune
        "justice"          -> Res.drawable.tarot_justice
        "hanged_man"       -> Res.drawable.tarot_hanged_man
        "death"            -> Res.drawable.tarot_death
        "temperance"       -> Res.drawable.tarot_temperance
        "devil"            -> Res.drawable.tarot_devil
        "tower"            -> Res.drawable.tarot_tower
        "star"             -> Res.drawable.tarot_star
        "moon"             -> Res.drawable.tarot_moon
        "sun"              -> Res.drawable.tarot_sun
        "judgment"         -> Res.drawable.tarot_judgment
        else               -> Res.drawable.tarot_world
    }
)

// ── Экран списка ──────────────────────────────────────────────────────────────

@Composable
fun TarotListScreen(
    vm: TarotViewModel,
    onPeriodSelected: (HoroscopePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // ── Декоративні зірки фону ────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            // Велика зірка — вгорі праворуч
            Image(
                painter            = painterResource(Res.drawable.ic_star),
                contentDescription = null,
                modifier           = Modifier
                    .size(70.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = 52.dp)
                    .graphicsLayer { alpha = 0.50f },
            )
            // Мала зірка — верх зліва
            Image(
                painter            = painterResource(Res.drawable.ic_star),
                contentDescription = null,
                modifier           = Modifier
                    .size(14.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 18.dp, y = 130.dp)
                    .graphicsLayer { alpha = 0.18f },
            )
            // Маленька зірка — праворуч посередині
            Image(
                painter            = painterResource(Res.drawable.ic_star),
                contentDescription = null,
                modifier           = Modifier
                    .size(9.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-48).dp, y = 160.dp)
                    .graphicsLayer { alpha = 0.14f },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxl)
            ) {
                // Декоративные карты справа
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 0.dp, top = 0.dp)
                        .size(width = 150.dp, height = 160.dp)
                ) {
                    // Задняя карта (наклон +12°)
                    Box(
                        modifier = Modifier
                            .size(width = 76.dp, height = 116.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-10).dp, y = 10.dp)
                            .rotate(12f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.tarot_world),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                    }
                    // Передняя карта (наклон -6°)
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 122.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 10.dp, y = 0.dp)
                            .rotate(-6f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, AppColors.AccentGold.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.tarot_sun),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Текст
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .padding(start = Spacing.xl, top = 4.dp)
                ) {
                    SectionLabel(str.tarot_label)
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = str.tarot_title1,
                        fontSize = AppType.h1,
                        fontWeight = FontWeight.Light,
                        color = AppColors.TextPrimary,
                    )
                    Text(
                        text = str.tarot_title2,
                        fontSize = AppType.h1,
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        color = AppColors.AccentGold,
                    )
                    Spacer(Modifier.height(Spacing.s))
                    // Декоративный разделитель
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.height(1.dp).width(24.dp).background(
                            Brush.horizontalGradient(listOf(Color.Transparent, AppColors.AccentGold.copy(0.5f)))
                        ))
                        Text("✦", color = AppColors.AccentGold.copy(0.7f), fontSize = 8.sp)
                        Box(Modifier.height(1.dp).width(24.dp).background(
                            Brush.horizontalGradient(listOf(AppColors.AccentGold.copy(0.5f), Color.Transparent))
                        ))
                    }
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        text = str.tarot_select_period,
                        fontSize = AppType.caption,
                        color = AppColors.TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // ── Period cards ──────────────────────────────────────────────
            val periods = listOf(
                Triple(HoroscopePeriod.DAILY,   str.tarot_period_day_title,   str.tarot_period_day_desc),
                Triple(HoroscopePeriod.WEEKLY,  str.tarot_period_week_title,  str.tarot_period_week_desc),
                Triple(HoroscopePeriod.MONTHLY, str.tarot_period_month_title, str.tarot_period_month_desc),
            )

            Column(
                modifier = Modifier.padding(horizontal = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                periods.forEach { (period, title, desc) ->
                    val snapshot = state.periodSnapshots[period.id]
                    val savedCards: List<TarotCard> = remember(snapshot) {
                        snapshot?.cards?.mapNotNull { snap ->
                            ALL_TAROT.find { it.number == snap.number }?.copy(reversed = snap.reversed)
                        } ?: emptyList()
                    }
                    PeriodCard(
                        period     = period,
                        title      = title,
                        desc       = desc,
                        savedCards = savedCards,
                        onClick    = { onPeriodSelected(period) },
                    )
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ── Карточка периода ──────────────────────────────────────────────────────────

@Composable
private fun PeriodCard(
    period:     HoroscopePeriod,
    title:      String,
    desc:       String,
    savedCards: List<TarotCard>,
    onClick:    () -> Unit,
) {
    val hasSaved = savedCards.isNotEmpty()

    val glowAlpha by rememberInfiniteTransition(label = "cg${period.id}").animateFloat(
        initialValue  = 0.25f,
        targetValue   = 0.65f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "cga${period.id}",
    )
    val borderAlpha = if (hasSaved) glowAlpha else 0.22f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF16130E), Color(0xFF0C0A07))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AppColors.AccentGold.copy(alpha = borderAlpha),
                        AppColors.AccentGold.copy(alpha = borderAlpha * 0.4f),
                        AppColors.AccentGold.copy(alpha = borderAlpha * 0.6f),
                    )
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Иконка периода ────────────────────────────────────────────
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Внешнее золотое свечение
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AppColors.AccentGold.copy(alpha = 0.10f),
                                    Color.Transparent,
                                )
                            )
                        )
                )
                Image(
                    painter = painterResource(
                        when (period) {
                            HoroscopePeriod.DAILY   -> Res.drawable.tarot_icon_day
                            HoroscopePeriod.WEEKLY  -> Res.drawable.tarot_icon_week
                            HoroscopePeriod.MONTHLY -> Res.drawable.tarot_icon_month
                        }
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // ── Заголовок и подпись ───────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = desc,
                    fontSize = 12.sp,
                    color    = AppColors.TextMuted,
                    lineHeight = 16.sp,
                )
                if (hasSaved) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AppColors.AccentGold.copy(alpha = 0.8f))
                        )
                        Text(
                            text     = "✦",
                            fontSize = 8.sp,
                            color    = AppColors.AccentGold.copy(0.6f),
                        )
                    }
                }
            }

            // ── Мини-карты + шеврон ───────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                repeat(3) { i ->
                    MiniCardSlot(card = savedCards.getOrNull(i))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = "›",
                    fontSize = 22.sp,
                    color    = AppColors.AccentGold.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }
}

// ── Мини-карта ────────────────────────────────────────────────────────────────

@Composable
private fun MiniCardSlot(card: TarotCard?) {
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1508), Color(0xFF0A0804))
                )
            )
            .border(
                width = 0.8.dp,
                color = if (card != null) AppColors.AccentGold.copy(alpha = 0.55f)
                        else AppColors.AccentGold.copy(alpha = 0.18f),
                shape = RoundedCornerShape(5.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (card != null) {
            Image(
                painter            = card.painter(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = if (card.reversed) 180f else 0f },
            )
            Box(Modifier.fillMaxSize().background(AppColors.AccentGold.copy(alpha = 0.06f)))
        } else {
            // Порожній слот — рубашка карти приглушена
            Image(
                painter            = painterResource(Res.drawable.back_card),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.42f },
            )
        }
    }
}

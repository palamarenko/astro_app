package com.astro.app.ui.screens.tarot

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import astroapp.composeapp.generated.resources.*
import com.astro.app.data.HoroscopePeriod
import com.astro.app.data.TarotCard
import com.astro.app.data.ALL_TAROT
import com.astro.app.i18n.str
import com.astro.app.ui.components.SectionLabel
import com.astro.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource


// ── Маппинг resourceKey → DrawableResource (те же PNG что в CardFront) ────────

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
    ) {
        Spacer(Modifier.height(Spacing.xxl))
        SectionLabel(str.tarot_label)
        Spacer(Modifier.height(Spacing.m))
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
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            text = str.tarot_select_period,
            fontSize = AppType.caption,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(Spacing.xl))

        val periods = listOf(
            Triple(HoroscopePeriod.DAILY,   str.tarot_period_day_title,   str.tarot_period_day_desc),
            Triple(HoroscopePeriod.WEEKLY,  str.tarot_period_week_title,  str.tarot_period_week_desc),
            Triple(HoroscopePeriod.MONTHLY, str.tarot_period_month_title, str.tarot_period_month_desc),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            periods.forEach { (period, title, desc) ->
                val snapshot = state.periodSnapshots[period.id]
                val savedCards: List<TarotCard> = remember(snapshot) {
                    snapshot?.cards?.mapNotNull { snap ->
                        ALL_TAROT.find { it.number == snap.number }?.copy(reversed = snap.reversed)
                    } ?: emptyList()
                }
                PeriodRow(
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

// ── Строка периода ────────────────────────────────────────────────────────────

@Composable
private fun PeriodRow(
    period:     HoroscopePeriod,
    title:      String,
    desc:       String,
    savedCards: List<TarotCard>,
    onClick:    () -> Unit,
) {
    val hasSaved = savedCards.isNotEmpty()

    val glowAlpha by rememberInfiniteTransition(label = "rowGlow${period.id}").animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "rowGlowA${period.id}"
    )

    val periodColor = when (period) {
        HoroscopePeriod.DAILY   -> AppColors.AccentGold
        HoroscopePeriod.WEEKLY  -> AppColors.Water
        HoroscopePeriod.MONTHLY -> AppColors.Earth
    }
    val periodIcon = when (period) {
        HoroscopePeriod.DAILY   -> "☀"
        HoroscopePeriod.WEEKLY  -> "☾"
        HoroscopePeriod.MONTHLY -> "✦"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.m))
            .background(AppColors.Card)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        periodColor.copy(alpha = if (hasSaved) glowAlpha * 0.6f else 0.15f),
                        periodColor.copy(alpha = if (hasSaved) glowAlpha * 0.3f else 0.08f),
                    )
                ),
                shape = RoundedCornerShape(Radius.m)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Иконка периода
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Radius.s))
                    .background(periodColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = periodIcon,
                    fontSize = TextUnit(20f, TextUnitType.Sp),
                    color    = periodColor,
                )
            }

            // Заголовок и подпись
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    fontSize   = AppType.body,
                    fontWeight = FontWeight.Medium,
                    color      = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = desc,
                    fontSize = AppType.caption,
                    color    = AppColors.TextMuted,
                )
            }

            // Мини-карты + шеврон
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                repeat(3) { i ->
                    MiniCardSlot(
                        card        = savedCards.getOrNull(i),
                        accentColor = periodColor,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text     = "›",
                    fontSize = TextUnit(20f, TextUnitType.Sp),
                    color    = AppColors.TextDim,
                )
            }
        }
    }
}

// ── Мини-карта ────────────────────────────────────────────────────────────────

@Composable
private fun MiniCardSlot(
    card:        TarotCard?,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .width(26.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1B42), Color(0xFF090912))
                )
            )
            .border(
                width = 0.8.dp,
                color = if (card != null) accentColor.copy(alpha = 0.50f)
                        else AppColors.Border.copy(alpha = 0.35f),
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (card != null) {
            // Реальное изображение карты
            Image(
                painter            = card.painter(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Перевёрнутые карты — поворачиваем на 180°
                        rotationZ = if (card.reversed) 180f else 0f
                    },
            )
            // Тонкий цветной оверлей для индикации периода
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.08f))
            )
        } else {
            // Заглушка — рубашка карты (звезда)
            Text(
                text     = "✦",
                fontSize = TextUnit(8f, TextUnitType.Sp),
                color    = AppColors.AccentGold.copy(alpha = 0.22f),
            )
        }
    }
}

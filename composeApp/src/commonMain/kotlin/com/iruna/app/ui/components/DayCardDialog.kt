@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.iruna.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iruna.app.data.TarotCard
import com.iruna.app.i18n.localizedKeywords
import com.iruna.app.i18n.localizedName
import com.iruna.app.i18n.str
import com.iruna.app.ui.theme.*
import iruna.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * Попап «Карта дня» — показывает выбранную по дате таро-карту и короткий
 * прогноз на сегодня. Открывается по кнопке [DayCardButton] на экране гороскопов.
 *
 * При первом открытии карты в этот день ([reveal] = true) проигрывается анимация
 * раскрытия как на экране таро: карта дрожит рубашкой вверх ~1.5 с и переворачивается
 * лицом. При повторных открытиях карта сразу показана лицом вверх.
 */
@Composable
fun DayCardDialog(
    card: TarotCard,
    text: String?,
    loading: Boolean,
    reveal: Boolean,
    onDismiss: () -> Unit,
) {
    val gold = AppColors.AccentGold

    val inf = rememberInfiniteTransition(label = "dayCardDlg")
    val float by inf.animateFloat(
        -4f, 4f,
        infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), "float"
    )
    val glow by inf.animateFloat(
        0.45f, 0.9f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), "glow"
    )

    // ── Состояние анимации раскрытия ──────────────────────────────────────────
    // frontUp — карта повёрнута лицом (флип завершён). shaking — дрожит рубашкой вверх.
    var frontUp by remember { mutableStateOf(!reveal) }
    var shaking by remember { mutableStateOf(reveal) }
    LaunchedEffect(Unit) {
        if (reveal) {
            delay(1500)          // тряска рубашкой вверх — как на экране таро
            shaking = false
            frontUp = true       // запускаем переворот
        }
    }
    val flip by animateFloatAsState(
        targetValue = if (frontUp) 180f else 0f,
        animationSpec = tween(760, easing = FastOutSlowInEasing),
        label = "flip",
    )
    val showingFront = flip > 90f

    // Дрожание рубашкой вверх — те же keyframes, что на экране таро
    val shakeX by inf.animateFloat(
        0f, 0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 520
                0f    at 0   using LinearEasing
                (-6f) at 60  using FastOutSlowInEasing
                6f    at 160 using FastOutSlowInEasing
                (-4f) at 240 using FastOutSlowInEasing
                4f    at 320 using FastOutSlowInEasing
                (-2f) at 400 using FastOutSlowInEasing
                0f    at 460 using LinearEasing
                0f    at 520
            },
            RepeatMode.Restart,
        ),
        "shakeX",
    )
    val shakeRot by inf.animateFloat(
        0f, 0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 520
                0f      at 0
                (-2.2f) at 80  using FastOutSlowInEasing
                2.2f    at 200 using FastOutSlowInEasing
                (-1.4f) at 320 using FastOutSlowInEasing
                0f      at 460
                0f      at 520
            },
            RepeatMode.Restart,
        ),
        "shakeRot",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(enabled = true) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1A1525), Color(0xFF0D0D18)))
                    )
                    .border(1.dp, gold.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                    // перехватываем клики, чтобы тап по карточке не закрывал попап
                    .clickable(enabled = false) { }
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // ── Заголовок «Карта дня» ─────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.width(24.dp).height(1.dp).background(gold.copy(alpha = 0.4f)))
                        Text(
                            text = str.daycard_label.uppercase(),
                            fontSize = 12.sp,
                            color = gold,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = TextUnit(0.22f, TextUnitType.Em),
                        )
                        Box(Modifier.width(24.dp).height(1.dp).background(gold.copy(alpha = 0.4f)))
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = str.daycard_subtitle,
                        fontSize = 12.sp,
                        color = AppColors.TextMuted,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Карта: рубашка → флип → лицо ──────────────────────────
                    Box(contentAlignment = Alignment.Center) {
                        // золотое свечение за картой
                        Box(
                            modifier = Modifier
                                .size(width = 168.dp, height = 252.dp)
                                .graphicsLayer { alpha = 0.35f + 0.35f * glow }
                                .background(
                                    Brush.radialGradient(listOf(gold.copy(alpha = 0.30f), Color.Transparent)),
                                    RoundedCornerShape(20.dp),
                                )
                        )
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = float
                                    if (shaking) {
                                        translationX = shakeX
                                        rotationZ = shakeRot
                                    }
                                    rotationY = flip
                                    cameraDistance = 14f * density
                                }
                                .width(150.dp)
                                .aspectRatio(2f / 3f),
                        ) {
                            if (!showingFront) {
                                // ── Рубашка ───────────────────────────────────
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                ) {
                                    Image(
                                        painter = painterResource(Res.drawable.back_card),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            } else {
                                // ── Лицо карты (контр-поворот, чтобы не зеркалилось) ─
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationY = 180f }
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                ) {
                                    Image(
                                        painter = dayCardPainter(card.resourceKey),
                                        contentDescription = card.localizedName(),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    }

                    // ── Название, ключевые слова и прогноз — появляются после переворота ─
                    AnimatedVisibility(
                        visible = showingFront,
                        enter = fadeIn(tween(450)) + expandVertically(tween(400)),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = card.localizedName(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Light,
                                color = AppColors.TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = card.localizedKeywords(),
                                fontSize = 11.sp,
                                color = gold.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                letterSpacing = TextUnit(0.04f, TextUnitType.Em),
                            )

                            Spacer(Modifier.height(18.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    loading -> LoadingDots()
                                    else -> Text(
                                        text = text ?: str.daycard_empty,
                                        fontSize = 14.5.sp,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Light,
                                        color = Color(0xFFD8D0C0),
                                        lineHeight = TextUnit(23f, TextUnitType.Sp),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }

                            Spacer(Modifier.height(22.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(gold.copy(alpha = 0.12f))
                                    .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { onDismiss() }
                                    .padding(horizontal = 32.dp, vertical = 11.dp),
                            ) {
                                Text(
                                    text = str.daycard_close,
                                    fontSize = 13.sp,
                                    color = gold,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = TextUnit(0.06f, TextUnitType.Em),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── resourceKey → DrawableResource ────────────────────────────────────────────
@Composable
private fun dayCardPainter(resourceKey: String) = painterResource(
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

package com.iruna.app.ui.screens.horoscope

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

import com.iruna.app.data.ALL_SIGNS
import com.iruna.app.data.ZodiacSign
import com.iruna.app.i18n.*
import com.iruna.app.ui.components.SectionLabel
import com.iruna.app.ui.theme.*


@Composable
fun SignPickerScreen(
    onSignSelected: (ZodiacSign) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl)
        ) {
            Spacer(Modifier.height(16.dp))
            SectionLabel(str.sign_picker_label)
            Spacer(Modifier.height(Spacing.m))
            Text(text = str.sign_picker_title1, fontSize = AppType.h1, fontWeight = FontWeight.Light, color = AppColors.TextPrimary)
            Text(text = str.sign_picker_title2, fontSize = AppType.h1, fontWeight = FontWeight.Light, fontStyle = FontStyle.Italic, color = AppColors.TextPrimary)
            Spacer(Modifier.height(Spacing.s))
            Text(text = str.sign_picker_subtitle, fontSize = AppType.caption, color = AppColors.TextDim)
            Spacer(Modifier.height(Spacing.xxl))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.height(((ALL_SIGNS.size / 3) * (90 + 9)).dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(ALL_SIGNS) { index, sign ->
                    SignCard(sign = sign, index = index, onClick = { onSignSelected(sign) })
                }
            }
        }
    }
}

@Composable
private fun SignCard(sign: ZodiacSign, index: Int, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 25L)
        visible = true
    }
    val elementColor = AppColors.elementColor(sign.element)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(animationSpec = tween(350)) { it / 5 },
    ) {
        var hovered by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (hovered) elementColor.copy(alpha = 0.07f) else AppColors.Card)
                .border(
                    width = 1.dp,
                    color = if (hovered) elementColor.copy(alpha = 0.33f) else AppColors.Border,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { hovered = true; onClick() }
                .padding(horizontal = 6.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = sign.iconPainter(),
                    contentDescription = sign.localizedName(),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(text = sign.localizedName(), fontSize = AppType.caption, fontWeight = FontWeight.Normal, color = AppColors.TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(text = sign.localizedElement(), fontSize = TextUnit(9f, TextUnitType.Sp), color = elementColor)
            }
        }
    }
}

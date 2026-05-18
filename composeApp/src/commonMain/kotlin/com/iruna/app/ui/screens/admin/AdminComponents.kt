package com.iruna.app.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iruna.app.ui.theme.*

// ── Shared admin tab item ─────────────────────────────────────────────────────

@Composable
internal fun AdminTabItem(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.s))
            .background(if (active) AppColors.AccentGold else AppColors.Surface)
            .then(
                if (!active) Modifier.border(1.dp, AppColors.AccentGold.copy(alpha = 0.3f), RoundedCornerShape(Radius.s))
                else Modifier
            )
            .clickable(enabled = !active, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (active) Color(0xFF0A0A0F) else AppColors.AccentGold,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ── Sub-tab button ────────────────────────────────────────────────────────────

@Composable
internal fun SubTabButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.s))
            .background(if (active) Color(0xFF9B6DFF).copy(alpha = 0.18f) else AppColors.Surface)
            .border(1.dp, if (active) Color(0xFF9B6DFF).copy(alpha = 0.6f) else AppColors.Border, RoundedCornerShape(Radius.s))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) Color(0xFFB89EFF) else AppColors.TextMuted,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            fontSize = 12.sp,
        )
    }
}

// ── Generic reusable components ───────────────────────────────────────────────

@Composable
internal fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(if (selected) AppColors.AccentGold else AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF0A0A0F) else AppColors.TextMuted,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 12.sp,
        )
    }
}

@Composable
internal fun NavArrow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(Radius.s))
            .background(AppColors.Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = AppColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.s))
            .background(if (enabled) AppColors.Surface else AppColors.CardDark)
            .border(1.dp, AppColors.AccentGold.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(Radius.s))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (enabled) AppColors.AccentGold else AppColors.TextDim, fontSize = 13.sp)
    }
}

@Composable
internal fun ControlLabel(text: String) {
    Text(text, color = AppColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
}

@Composable
internal fun StatusText(text: String, color: Color) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

// ── Save bar ──────────────────────────────────────────────────────────────────

@Composable
internal fun SaveBar(
    isSaving: Boolean,
    isLoading: Boolean,
    savedText: String?,
    errorText: String?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, AppColors.Background.copy(alpha = 0.97f)), startY = 0f, endY = 60f))
            .padding(horizontal = Spacing.xl)
            .padding(top = Spacing.xl, bottom = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = savedText != null || errorText != null,
            enter = fadeIn(tween(250)) + expandVertically(tween(250)),
            exit  = fadeOut(tween(200)) + shrinkVertically(tween(200)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    errorText != null -> Text(errorText, color = Color(0xFFEB5757), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    savedText != null -> Text(savedText, color = Color(0xFF6FCF97), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(Spacing.s))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.m))
                .background(if (!isSaving) AppColors.AccentGold else AppColors.Surface)
                .border(1.dp, AppColors.AccentGold.copy(alpha = if (!isSaving) 0f else 0.3f), RoundedCornerShape(Radius.m))
                .clickable(enabled = !isSaving && !isLoading) { onSave() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isSaving) "Saving..." else "Save All",
                color = if (!isSaving) Color(0xFF0A0A0F) else AppColors.TextMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
        }
    }
}

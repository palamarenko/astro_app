package com.iruna.app.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.ktor2.KtorNetworkFetcherFactory
import com.iruna.app.googleMapsApiKey
import com.iruna.app.ui.theme.*

@Composable
fun PlacePickerDialog(
    vm: PlacePickerViewModel,
    onConfirm: (name: String, lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by vm.state.collectAsState()

    // ImageLoader — UI-инфраструктура, остаётся в composable
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Background)
                .border(1.dp, AppColors.AccentGold.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
        ) {
            Header(onDismiss)
            SearchField(state, vm)

            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            // Основной контент — занимает всё свободное место
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.showSuggestions -> SuggestionsList(state, vm)
                    state.selected != null -> SelectedPlaceView(state, imageLoader)
                    else -> EmptyState(state.query)
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Border))

            ConfirmButton(state) {
                val result = vm.buildResult() ?: return@ConfirmButton
                onConfirm(result.name, result.lat, result.lng)
            }
        }
    }
}

// ── Шапка ─────────────────────────────────────────────────────────────────────

@Composable
private fun Header(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("📍", fontSize = 18.sp)
        Text(
            "Место рождения",
            fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary, modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.s))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("✕", color = AppColors.TextDim, fontSize = 16.sp)
        }
    }
}

// ── Поле поиска ───────────────────────────────────────────────────────────────

@Composable
private fun SearchField(state: PlacePickerUiState, vm: PlacePickerViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Введите город или страну…", color = AppColors.TextDim, fontSize = 13.sp) },
            singleLine = true,
            leadingIcon = {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.AccentGold
                    )
                } else {
                    Text("🔍", fontSize = 14.sp)
                }
            },
            trailingIcon = {
                if (state.query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.s))
                            .clickable { vm.clearQuery() }
                            .padding(4.dp)
                    ) {
                        Text("✕", color = AppColors.TextDim, fontSize = 13.sp)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search
            ),
            textStyle = LocalTextStyle.current.copy(color = AppColors.TextPrimary, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = AppColors.AccentGold,
                unfocusedBorderColor    = AppColors.Border,
                cursorColor             = AppColors.AccentGold,
                focusedContainerColor   = AppColors.CardDark,
                unfocusedContainerColor = AppColors.CardDark,
            ),
            shape = RoundedCornerShape(Radius.s)
        )
    }
}

// ── Список подсказок ──────────────────────────────────────────────────────────

@Composable
private fun SuggestionsList(state: PlacePickerUiState, vm: PlacePickerViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(state.suggestions.size) { i ->
            val pred = state.suggestions[i]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.selectPrediction(pred) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📍", fontSize = 15.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pred.formatting.mainText.ifBlank { pred.description },
                        fontSize = 14.sp,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (pred.formatting.secondaryText.isNotBlank()) {
                        Text(
                            pred.formatting.secondaryText,
                            fontSize = 11.sp,
                            color = AppColors.TextDim
                        )
                    }
                }
            }
            if (i < state.suggestions.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                        .background(AppColors.Border)
                )
            }
        }
    }
}

// ── Выбранное место: карта + название ─────────────────────────────────────────

@Composable
private fun SelectedPlaceView(state: PlacePickerUiState, imageLoader: ImageLoader) {
    val selected = state.selected ?: return

    // Карта занимает всё доступное пространство
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.CardDark),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isFetchingDetails -> {
                CircularProgressIndicator(color = AppColors.AccentGold)
            }
            state.hasCoords -> {
                val mapUrl = state.staticMapUrl(googleMapsApiKey)
                AsyncImage(
                    model = mapUrl,
                    contentDescription = "Map of ${selected.description}",
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text("🗺️", fontSize = 48.sp)
            }
        }
    }
}

// ── Пустое состояние ──────────────────────────────────────────────────────────

@Composable
private fun EmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌍", fontSize = 40.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            if (query.length == 1) "Введите ещё символы…"
            else "Введите город или страну рождения",
            fontSize = 14.sp,
            color = AppColors.TextDim,
            textAlign = TextAlign.Center
        )
    }
}

// ── Кнопка подтверждения ──────────────────────────────────────────────────────

@Composable
private fun ConfirmButton(state: PlacePickerUiState, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.m))
                .background(if (state.canConfirm) AppColors.AccentGold else AppColors.Card)
                .border(
                    1.dp,
                    if (state.canConfirm) Color.Transparent else AppColors.Border,
                    RoundedCornerShape(Radius.m)
                )
                .clickable(enabled = state.canConfirm, onClick = onClick)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (state.isFetchingDetails) "Загрузка…" else "Подтвердить",
                color = if (state.canConfirm) Color(0xFF0A0A0F) else AppColors.TextDim,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}

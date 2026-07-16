package com.iruna.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/**
 * iOS-заглушка. Полноценный шеринг картинки (UIActivityViewController)
 * пока не реализован — по требованию будет добавлен позже.
 */
@Composable
actual fun rememberImageSharer(): ImageSharer = remember { NoopImageSharer }

private object NoopImageSharer : ImageSharer {
    override fun share(image: ImageBitmap, title: String) {
        // no-op на iOS до отдельной реализации
    }
}

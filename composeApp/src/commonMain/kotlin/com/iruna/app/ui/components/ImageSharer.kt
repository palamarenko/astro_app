package com.iruna.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Платформо-независимый интерфейс для «поделиться» готовой картинкой.
 * Android : сохраняет PNG в cache, отдаёт через FileProvider в Intent.ACTION_SEND.
 * iOS     : UIActivityViewController с UIImage.
 */
interface ImageSharer {
    /** Показать системный share-лист с картинкой [image]. */
    fun share(image: ImageBitmap, title: String)
}

/** Возвращает платформо-специфичный [ImageSharer]. */
@Composable
expect fun rememberImageSharer(): ImageSharer

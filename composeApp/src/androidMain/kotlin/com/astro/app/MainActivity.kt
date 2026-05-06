package com.astro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.astro.app.data.TarotStorageInitializer
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Подготавливаем хранилище состояний (Tarot daily lockout)
        TarotStorageInitializer.init(applicationContext)
        // Инициализируем Google Mobile Ads SDK
        MobileAds.initialize(this) {}
        setContent {
            App()
        }
    }
}

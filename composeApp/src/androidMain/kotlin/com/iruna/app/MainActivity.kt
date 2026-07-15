package com.iruna.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.iruna.app.data.AnalyticsInitializer
import com.iruna.app.data.TarotStorageInitializer
import com.iruna.app.notifications.NotificationHelper
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        TarotStorageInitializer.init(applicationContext)
        AnalyticsInitializer.init(applicationContext)
        MobileAds.initialize(this) {}
        NotificationHelper.createChannel(applicationContext)
        setContent {
            App()
        }
    }
}

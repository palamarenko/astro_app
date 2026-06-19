package com.iruna.app

import android.os.Build

actual object PlatformInfo {
    actual val versionName: String = BuildConfig.VERSION_NAME
    actual val versionCode: Int    = BuildConfig.VERSION_CODE
    actual val platformName: String = "Android"
    actual val osVersion: String   = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}

package com.iruna.app

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

actual object PlatformInfo {
    actual val versionName: String
        get() = NSBundle.mainBundle.infoDictionary
            ?.get("CFBundleShortVersionString") as? String ?: "—"

    actual val versionCode: Int
        get() = (NSBundle.mainBundle.infoDictionary
            ?.get("CFBundleVersion") as? String)?.toIntOrNull() ?: 0

    actual val platformName: String = "iOS"

    actual val osVersion: String
        get() = "iOS ${UIDevice.currentDevice.systemVersion}"
}

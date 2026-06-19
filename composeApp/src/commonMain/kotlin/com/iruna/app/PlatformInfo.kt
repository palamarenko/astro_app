package com.iruna.app

/** Platform-agnostic app version info. */
expect object PlatformInfo {
    val versionName: String
    val versionCode: Int
    val platformName: String  // "Android" or "iOS"
    val osVersion: String
}

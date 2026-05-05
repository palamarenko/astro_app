package com.astro.app

// Injected at iOS build time via Info.plist or environment variable.
// Set ANTHROPIC_API_KEY in your Xcode scheme environment variables.
actual val anthropicApiKey: String =
    platform.Foundation.NSBundle.mainBundle
        .objectForInfoDictionaryKey("ANTHROPIC_API_KEY") as? String ?: ""

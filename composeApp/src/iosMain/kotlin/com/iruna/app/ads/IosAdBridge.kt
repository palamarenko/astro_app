package com.iruna.app.ads

interface AdRewardCallback {
    fun onRewarded()
    fun onFailed()
}

interface IosAdDelegate {
    val isAdReady: Boolean
    fun preload()
    fun showAd(callback: AdRewardCallback)
}

object IosAdBridge {
    var delegate: IosAdDelegate? = null
}

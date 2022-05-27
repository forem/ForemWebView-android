package com.forem.webview

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForemWebViewSession @Inject constructor() {

    lateinit var androidWebViewBridge: AndroidWebViewBridge

    fun videoPlayerPaused() {
        androidWebViewBridge.videoPlayerPaused()
    }

    fun videoPlayerTimerUpdate(seconds: String) {
        androidWebViewBridge.updateTimer(seconds)
    }
}

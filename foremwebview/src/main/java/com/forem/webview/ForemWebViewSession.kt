package com.forem.webview

import android.util.Log

class ForemWebViewSession {

    var androidWebViewBridge: AndroidWebViewBridge? = null

    private var instance: ForemWebViewSession? = null

    fun getInstance(): ForemWebViewSession? {
        if (instance == null) {
            synchronized(ForemWebViewSession::class.java) {
                if (instance == null) {
                    instance = ForemWebViewSession()
                }
            }
        }
        return instance
    }

    fun videoPlayerPaused() {
        if (androidWebViewBridge == null) {
            Log.e("ForemWebViewSession", "videoPlayerPaused: androidWebViewBridge is null")
        }
        androidWebViewBridge?.videoPlayerPaused()
    }

    fun videoPlayerTimerUpdate(seconds: String) {
        if (androidWebViewBridge == null) {
            Log.e("ForemWebViewSession", "videoPlayerTimerUpdate: androidWebViewBridge is null")
        }
        androidWebViewBridge?.updateTimer(seconds)
    }
}

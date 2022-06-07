package com.forem.webview

public class ForemWebViewSession {

    lateinit var androidWebViewBridge: AndroidWebViewBridge

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
        androidWebViewBridge.videoPlayerPaused()
    }

    fun videoPlayerTimerUpdate(seconds: String) {
        androidWebViewBridge.updateTimer(seconds)
    }
}

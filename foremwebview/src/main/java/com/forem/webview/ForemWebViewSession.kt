package com.forem.webview

import android.util.Log

/**
 * Helper class to play and pause video in [VideoPlayerActivity] and send that information to
 * website.
 */
class ForemWebViewSession {

    /** The [AndroidWebViewBridge] instance required to call javascript interface functions. */
    var androidWebViewBridge: AndroidWebViewBridge? = null

    private var instance: ForemWebViewSession? = null

    /**
     * Function which creates a new instance of this class or shares the old instance.
     *
     * @return instance of [ForemWebViewSession].
     */
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

    /** Gets called whenever the video gets paused or the [VideoPlayerActivity] is destroyed. */
    fun videoPlayerPaused() {
        if (androidWebViewBridge == null) {
            Log.e("ForemWebViewSession", "videoPlayerPaused: androidWebViewBridge is null")
        }
        androidWebViewBridge?.videoPlayerPaused()
    }

    /**
     * This function regularly updates the timer for the video on backend.
     *
     * @return seconds at which the video is at currently.
     */
    fun videoPlayerTimerUpdate(seconds: String) {
        if (androidWebViewBridge == null) {
            Log.e("ForemWebViewSession", "videoPlayerTimerUpdate: androidWebViewBridge is null")
        }
        androidWebViewBridge?.updateTimer(seconds)
    }

    /** Gets called whenever the podcast gets paused. */
    fun podcastPaused() {
        Log.d("TAGG","ForemWebViewSession: podcastPaused")
        if (androidWebViewBridge == null) {
            Log.e("ForemWebViewSession", "podcastPaused: androidWebViewBridge is null")
        }
        androidWebViewBridge?.podcastPaused()
    }
}

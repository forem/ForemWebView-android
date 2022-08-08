package com.forem.webview

/**
 * Helper class to play and pause video in [VideoPlayerActivity] and send that information to
 * website.
 */
class ForemWebViewSession private constructor() {

    companion object {
        @Volatile
        private lateinit var instance: ForemWebViewSession

        /** Returns the single instance of [ForemWebViewSession]. */
        fun getInstance(): ForemWebViewSession {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = ForemWebViewSession()
                }
                return instance
            }
        }
    }

    private var androidWebViewBridge: AndroidWebViewBridge? = null

    fun setAndroidWebViewBridge(androidWebViewBridge: AndroidWebViewBridge) {
        this.androidWebViewBridge = androidWebViewBridge
    }

    /** Gets called whenever the video gets paused or the [VideoPlayerActivity] is destroyed. */
    fun videoPlayerPaused() {
        androidWebViewBridge?.videoPlayerPaused()
    }

    /**
     * This function regularly updates the timer for the video on backend.
     *
     * @return seconds at which the video is at currently.
     */
    fun videoPlayerTimerUpdate(seconds: String) {
        androidWebViewBridge?.updateTimer(seconds)
    }

    /** Gets called whenever the podcast gets played. */
    fun podcastPlayed() {
        androidWebViewBridge?.podcastPlayed()
    }

    /** Gets called whenever the podcast gets paused. */
    fun podcastPaused() {
        androidWebViewBridge?.podcastPaused()
    }
}

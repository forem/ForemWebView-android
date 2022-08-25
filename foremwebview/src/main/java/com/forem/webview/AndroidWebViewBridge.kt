package com.forem.webview

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.forem.webview.media.AudioService
import com.forem.webview.video.VideoPlayerActivity
import com.google.gson.Gson
import org.json.JSONObject
import java.util.*

/** Bridge between WebView and its client where all the javascript interfaces are designed. */
class AndroidWebViewBridge(
    private val context: Activity,
    private val webViewClient: ForemWebViewClient
) {

    private var timer: Timer? = null

    // This queue maintains the list of all pending actions that needs to be executed by
    // AudioService. If we do not use this list and audioService is null then some of the actions
    // may fail.
    private val pendingPodcastActionsQueue: Queue<String> = LinkedList()

    // AudioService is initialized when onServiceConnected is executed after/during binding is done.
    private var audioService: AudioService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.service
            // Once the audio service is bind-ed, execute all pending actions.
            for(action in pendingPodcastActionsQueue){
                podcastMessage(action)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
        }
    }

    /**
     * Function which gets called by website when an error occurs.
     *
     * @param errorTag tag required for log message.
     * @param errorMessage message required for log message.
     */
    @JavascriptInterface
    fun logError(errorTag: String, errorMessage: String) {
    }

    /**
     * Function which gets called once the user has successfully logged-in.
     *
     * @param message contains user related meta data like name, id, etc.
     */
    @JavascriptInterface
    fun userLoginMessage(message: String) {
        val userDataObject = JSONObject(message)
        val name = userDataObject.optString("name")
        if (!name.isNullOrEmpty()) {
            // User logged in
            webViewClient.userLoggedIn(userDataObject.optInt("id"))
        }
    }

    /**
     * Function which gets called once the user has successfully logged-out.
     *
     * @param message contains user related data.
     */
    @JavascriptInterface
    fun userLogoutMessage(message: String) {
        // User logged-out
        webViewClient.userLoggedOut()
    }

    /**
     * Function which gets triggered when user does any action related to podcast.
     *
     * @param message contains information related to podcast like its url, play action, pause
     *    action, title, etc. which can be used to load th podcast in audio service.
     */
    @JavascriptInterface
    fun podcastMessage(message: String) {
        // Reference: https://stackoverflow.com/questions/9446868/access-ui-from-javascript-on-android
        context.runOnUiThread {
            var map: Map<String, String> = HashMap()
            map = Gson().fromJson(message, map.javaClass)
            when (map["action"]) {
                "load" -> loadPodcast(map["url"])
                "play" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.play(map["url"], map["seconds"])
                }
                "pause" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.pause()
                }
                "seek" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.seekTo(map["seconds"])
                }
                "rate" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.rate(map["rate"])
                }
                "muted" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.mute(map["muted"])
                }
                "volume" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.volume(map["volume"])
                }
                "metadata" -> {
                    if (audioService == null) {
                        pendingPodcastActionsQueue.add(message)
                    }
                    audioService?.loadMetadata(
                        map["episodeName"],
                        map["podcastName"],
                        map["podcastImageUrl"]
                    )
                }
                "terminate" -> terminatePodcast()
                else -> logError("Podcast Error", "Unknown action")
            }
        }
    }

    private fun loadPodcast(url: String?) {
        if (url == null) return

        if (audioService != null && timer != null) {
            return
        }

        AudioService.newIntent(context, url).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        // Clear out lingering timer if it exists & recreate
        timer?.cancel()
        timer = Timer()
        timer?.schedule(PodcastTimeUpdate(), 0, 1000)
    }

    private fun podcastTimeUpdate() {
        audioService?.let {
            val time = it.currentTimeInSec() / 1000
            val duration = it.durationInSec() / 1000
            if (duration < 0) {
                // The duration overflows into a negative when waiting to load audio (initializing)
                webViewClient.sendBridgeMessage(
                    BridgeMessageType.PODCAST,
                    mapOf("action" to "init")
                )
            } else {
                val message =
                    mapOf("action" to "tick", "duration" to duration, "currentTime" to time)
                webViewClient.sendBridgeMessage(BridgeMessageType.PODCAST, message)
            }
        }
    }

    /**
     * Function which gets triggered when user first clicks on video in webview player.
     *
     * @param message contains information like video url and current-time in video.
     */
    @JavascriptInterface
    fun videoMessage(message: String) {
        var map: Map<String, String> = HashMap()
        map = Gson().fromJson(message, map.javaClass)
        when (map["action"]) {
            "play" -> playVideo(map["url"], map["seconds"])
            else -> logError("Video Error", "Unknown action")
        }
    }

    private fun playVideo(url: String?, seconds: String?) {
        url?.let {
            context.runOnUiThread {
                audioService?.pause()
            }

            val intent = VideoPlayerActivity.newInstance(context, url, seconds ?: "0")
            context.startActivity(intent)
        }
    }

    /**
     * This method is used to open the native share-sheet of Android when simple text is to be
     * shared from the web-view.
     *
     * @param text contains the text which needs to be shared natively.
     */
    @JavascriptInterface
    fun shareText(text: String) {
        val shareIntent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            },
            null
        )
        context.startActivity(shareIntent)
    }

    /**
     * This function copies the text natively to clipboard.
     *
     * @param copyText contains the text which gets saved to clipboard natively.
     */
    @JavascriptInterface
    fun copyToClipboard(copyText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = ClipData.newPlainText("Forem", copyText)
        clipboard?.setPrimaryClip(clipData)
    }

    /**
     * Shows toast in android app triggered via website.
     *
     * @param message required to be displayed in Toast.
     */
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Helps in sending the timer value for the video to the backend.
     *
     * @param seconds new updated value of current time in video.
     */
    fun updateTimer(seconds: String) {
        val message = mapOf("action" to "tick", "currentTime" to seconds)
        webViewClient.sendBridgeMessage(BridgeMessageType.VIDEO, message)
    }

    /**
     * Helps in sending the information to webview that the video has been paused.
     */
    fun videoPlayerPaused() {
        webViewClient.sendBridgeMessage(BridgeMessageType.VIDEO, mapOf("action" to "pause"))
    }

    /**
     * Helps in sending the information to webview that the podcast is played.
     */
    fun podcastPlayed() {
        webViewClient.sendBridgeMessage(BridgeMessageType.PODCAST, mapOf("action" to "play"))
    }

    /**
     * Helps in sending the information to webview that the podcast has been paused.
     */
    fun podcastPaused() {
        webViewClient.sendBridgeMessage(BridgeMessageType.PODCAST, mapOf("action" to "pause"))
    }

    private fun terminatePodcast() {
        timer?.cancel()
        timer = null
        audioService?.let {
            it.clearNotification()
            context.unbindService(connection)
            context.stopService(Intent(context, AudioService::class.java))
            audioService = null
        }
    }

    inner class PodcastTimeUpdate : TimerTask() {
        override fun run() {
            context.runOnUiThread {
                podcastTimeUpdate()
            }
        }
    }
}

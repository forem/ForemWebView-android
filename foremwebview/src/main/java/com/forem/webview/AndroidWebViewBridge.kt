package com.forem.webview

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.forem.webview.media.AudioService
import com.forem.webview.video.VideoPlayerActivity
import com.google.gson.Gson
import org.json.JSONObject
import java.util.*

class AndroidWebViewBridge(
    private val context: Activity,
    private val webViewClient: ForemWebViewClient
) {

    private var timer: Timer? = null

    // AudioService is initialized when onServiceConnected is executed after/during binding is done.
    private var audioService: AudioService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.AudioServiceBinder
            audioService = binder.service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
        }
    }

    @JavascriptInterface
    fun processForemMetaData(foremMetaDataJsonObject: String) {
        var foremMetaDataMap: Map<String, String> = HashMap()
        foremMetaDataMap = Gson().fromJson(foremMetaDataJsonObject, foremMetaDataMap.javaClass)
        webViewClient.foremMetaDataReceived(foremMetaDataMap)
    }

    @JavascriptInterface
    fun logError(errorTag: String, errorMessage: String) {
        Log.e(errorTag, errorMessage)
    }

    @JavascriptInterface
    fun userLoginMessage(message: String) {
        val userDataObject = JSONObject(message)
        val name = userDataObject.optString("name")
        if (!name.isNullOrEmpty()) {
            // User logged in
            webViewClient.userLoggedIn(userDataObject.optInt("id"))
        }
    }

    @JavascriptInterface
    fun userLogoutMessage(message: String) {
        // User logged-out
        webViewClient.userLoggedOut()
    }

    @JavascriptInterface
    fun podcastMessage(message: String) {
        // Reference: https://stackoverflow.com/questions/9446868/access-ui-from-javascript-on-android
        context.runOnUiThread {
            var map: Map<String, String> = java.util.HashMap()
            map = Gson().fromJson(message, map.javaClass)
            when (map["action"]) {
                "load" -> loadPodcast(map["url"])
                "play" -> audioService?.play(map["url"], map["seconds"])
                "pause" -> audioService?.pause()
                "seek" -> audioService?.seekTo(map["seconds"])
                "rate" -> audioService?.rate(map["rate"])
                "muted" -> audioService?.mute(map["muted"])
                "volume" -> audioService?.volume(map["volume"])
                "metadata" -> audioService?.loadMetadata(
                    map["episodeName"],
                    map["podcastName"],
                    map["imageUrl"]
                )
                "terminate" -> terminatePodcast()
                else -> logError("Podcast Error", "Unknown action")
            }
        }
    }

    private fun loadPodcast(url: String?) {
        if (url == null) return

        AudioService.newIntent(context, url).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        // Clear out lingering timer if it exists & recreate
        timer?.cancel()
        timer = Timer()
        timer?.schedule(PodcastTimeUpdate(), 0, 1000)
    }

    private fun terminatePodcast() {
        timer?.cancel()
        timer = null

        audioService?.let {
            it.pause()
            context.unbindService(connection)
            context.stopService(Intent(context, AudioService::class.java))
            audioService = null
        }
    }

    fun podcastTimeUpdate() {
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
            // Pause the audio player in case the user is currently listening to a audio (podcast)
            audioService?.pause()
            timer?.cancel()

            val intent = VideoPlayerActivity.newInstance(context, url, seconds ?: "0")
            context.startActivity(intent)
        }
    }

    /**
     * This method is used to open the native share-sheet of Android when simple text is to be
     * shared from the web-view.
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

    @JavascriptInterface
    fun copyToClipboard(copyText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = ClipData.newPlainText("Forem", copyText)
        clipboard?.setPrimaryClip(clipData)
    }

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun updateTimer(seconds: String) {
        val message = mapOf("action" to "tick", "currentTime" to seconds)
        webViewClient.sendBridgeMessage(BridgeMessageType.VIDEO, message)
    }

    fun videoPlayerPaused() {
        webViewClient.sendBridgeMessage(BridgeMessageType.VIDEO, mapOf("action" to "pause"))
    }

    inner class PodcastTimeUpdate : TimerTask() {
        override fun run() {
            context.runOnUiThread {
                podcastTimeUpdate()
            }
        }
    }
}

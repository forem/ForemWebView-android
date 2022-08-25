package com.forem.webview.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.forem.webview.ForemWebViewSession
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        val intentFilter = IntentFilter().apply {
            addAction(PlayerNotificationManager.ACTION_PLAY)
            addAction(PlayerNotificationManager.ACTION_PAUSE)
            addAction(PlayerNotificationManager.ACTION_FAST_FORWARD)
            addAction(PlayerNotificationManager.ACTION_REWIND)
            addAction(PlayerNotificationManager.ACTION_STOP)
        }
    }

    private lateinit var foremWebViewSession: ForemWebViewSession

    override fun onReceive(context: Context?, intent: Intent?) {
        foremWebViewSession = ForemWebViewSession.getInstance()

        when (intent?.action) {
            PlayerNotificationManager.ACTION_PLAY -> {
                foremWebViewSession.podcastPlayed()
            }
            PlayerNotificationManager.ACTION_PAUSE -> {
                foremWebViewSession.podcastPaused()
            }
            PlayerNotificationManager.ACTION_FAST_FORWARD -> {
            }
            PlayerNotificationManager.ACTION_REWIND -> {
            }
            PlayerNotificationManager.ACTION_STOP -> {
            }
        }
    }
}

package com.forem.webview.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
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
        val foremWebViewSessionInstance = ForemWebViewSession().getInstance()
        if (foremWebViewSessionInstance == null) {
            Log.d("TAGG", "onReceive: foremWebViewSessionInstance: null")
            return
        } else {
            foremWebViewSession = foremWebViewSessionInstance
        }

        when (intent?.action) {
            PlayerNotificationManager.ACTION_PLAY -> {
                Log.d("TAGG", "onReceive: PLAY")
            }
            PlayerNotificationManager.ACTION_PAUSE -> {
                Log.d("TAGG", "onReceive: PAUSE")
                foremWebViewSession.podcastPaused()
            }
            PlayerNotificationManager.ACTION_FAST_FORWARD -> {
                Log.d("TAGG", "onReceive: FORWARD")
            }
            PlayerNotificationManager.ACTION_REWIND -> {
                Log.d("TAGG", "onReceive: REWIND")
            }
            PlayerNotificationManager.ACTION_STOP -> {
                Log.d("TAGG", "onReceive: STOP")
                //do what you want here!!!
            }
        }
    }
}
package com.forem.webview.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.forem.webview.ForemWebViewSession
import com.forem.webview.R
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import java.util.*

/** Activity which displays video using [ExoPlayer]. */
public class VideoPlayerActivity : AppCompatActivity(), Player.EventListener {

    // TODO(#37): Implement picture-in-picture
    // TODO(#176): Transition from portrait to landscape results in video restart.
    companion object {
        const val VIDEO_URL_INTENT_EXTRA = "VideoPlayerActivity.video_url"
        const val VIDEO_TIME_INTENT_EXTRA = "VideoPlayerActivity.video_time"

        fun newInstance(context: Context, url: String, time: String): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(VIDEO_URL_INTENT_EXTRA, url)
            intent.putExtra(VIDEO_TIME_INTENT_EXTRA, time)
            return intent
        }
    }

    private lateinit var player: SimpleExoPlayer
    private val timer = Timer()

    lateinit var foremWebViewSession: ForemWebViewSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.video_player_activity)

        if (ForemWebViewSession().getInstance() == null) {
            finish()
            return
        } else {
            foremWebViewSession = ForemWebViewSession().getInstance()!!
        }

        val playerView = findViewById<PlayerView>(R.id.player_view)

        val url = intent.getStringExtra(VIDEO_URL_INTENT_EXTRA)
        val time = intent.getStringExtra(VIDEO_TIME_INTENT_EXTRA)

        val streamUri = Uri.parse(url)
        val dataSourceFactory: DataSource.Factory =
            DefaultHttpDataSourceFactory("DEV-Native-android")
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(streamUri)

        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        player.prepare(mediaSource)
        player.seekTo(time!!.toLong() * 1000)
        player.playWhenReady = true

        timer.schedule(TimerUpdate(), 0, 1000)
    }

    override fun onDestroy() {
        if (::player.isInitialized) {
            player.playWhenReady = false
        }
        timer.cancel()
        foremWebViewSession.videoPlayerPaused()
        super.onDestroy()
    }

    fun timeUpdate() {
        if (::player.isInitialized) {
            val milliseconds = player.currentPosition
            val currentTime = (milliseconds / 1000).toString()
            foremWebViewSession.videoPlayerTimerUpdate(currentTime)
        }
    }

    inner class TimerUpdate : TimerTask() {
        override fun run() {
            runOnUiThread {
                timeUpdate()
            }
        }
    }
}
